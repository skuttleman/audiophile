(ns com.ben-allred.audiophile.api.services.repositories.files.core
  (:require
    [com.ben-allred.audiophile.api.services.interactors.core :as int]
    [com.ben-allred.audiophile.api.services.interactors.protocols :as pint]
    [com.ben-allred.audiophile.api.services.repositories.core :as repos]
    [com.ben-allred.audiophile.api.services.repositories.files.queries :as q]
    [com.ben-allred.audiophile.common.utils.colls :as colls]
    [com.ben-allred.audiophile.common.utils.maps :as maps]
    [com.ben-allred.audiophile.common.utils.uuids :as uuids]
    [integrant.core :as ig]))

(defmacro ^:private with-async [fut & body]
  `(let [future# (future ~fut)]
     (try (let [result# (do ~@body)]
            @future#
            result#)
          (catch Throwable ex#
            (future-cancel future#)
            (throw ex#)))))

(defn ^:private access-project! [executor projects project-id user-id]
  (when (empty? (-> projects
                    (q/access! user-id)
                    (update :where conj [:= :projects.id project-id])
                    (->> (repos/execute! executor))))
    (throw (ex-info "User cannot access this project" (maps/->m project-id user-id)))))

(defn ^:private access-file! [executor projects file-id user-id]
  (when (empty? (-> projects
                    (q/access! user-id)
                    (q/with-file-access file-id)
                    (->> (repos/execute! executor))))
    (throw (ex-info "User cannot access this file" (maps/->m file-id user-id)))))

(defn ^:private create-version* [executor files file-versions file user-id file-id]
  (-> file-versions
      (q/insert-version file file-id user-id)
      (->> (repos/execute! executor)))
  (colls/only! (repos/execute! executor (q/select-one files file-id))))

(defn ^:private create-artifact* [executor {store :store/kv :entity/keys [artifacts]} artifact user-id]
  (let [key (str "artifacts/" (uuids/random))]
    (with-async
      (repos/put! store
                  key
                  (:tempfile artifact)
                  {:content-type   (:content-type artifact)
                   :content-length (:size artifact)
                   :metadata       {:filename (:filename artifact)}})
      {:artifact/id       (->> user-id
                               (q/insert-artifact artifacts (repos/uri store key) artifact)
                               (repos/execute! executor)
                               colls/only!
                               :id)
       :artifact/filename (:filename artifact)})))

(defn ^:private query-for-project* [{entity :entity/files} project-id user-id]
  (q/select-for-user entity project-id user-id))

(defn ^:private create-file* [executor {:entity/keys [files file-versions projects]} project-id file user-id]
  (access-project! executor projects project-id user-id)
  (-> files
      (q/insert {:name       (:file/name file)
                 :project-id project-id
                 :created-by user-id})
      (->> (repos/execute! executor))
      colls/only!
      :id
      (->> (create-version* executor files file-versions file user-id))))

(defn ^:private create-file-version* [executor {:entity/keys [files file-versions projects]} file-id version user-id]
  (access-file! executor projects file-id user-id)
  (create-version* executor files file-versions version user-id file-id))

(deftype FileAccessor [repo]
  pint/IAccessor
  (query-many [_ opts]
    (repos/transact! repo repos/->exec! query-for-project* (:project/id opts) (:user/id opts)))

  pint/IFileAccessor
  (create-artifact! [_ artifact opts]
    (repos/transact! repo create-artifact* artifact (:user/id opts)))
  (create-file! [_ project-id file opts]
    (repos/transact! repo create-file* project-id file (:user/id opts)))
  (create-file-version! [_ file-id version opts]
    (repos/transact! repo create-file-version* file-id version (:user/id opts))))

(defmethod ig/init-key ::model [_ {:keys [repo]}]
  (->FileAccessor repo))
