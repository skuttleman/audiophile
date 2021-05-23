(ns com.ben-allred.audiophile.api.services.repositories.files.core
  (:require
    [com.ben-allred.audiophile.api.services.interactors.protocols :as pint]
    [com.ben-allred.audiophile.api.services.repositories.core :as repos]
    [com.ben-allred.audiophile.api.services.repositories.files.queries :as q]
    [com.ben-allred.audiophile.common.utils.colls :as colls]
    [com.ben-allred.audiophile.common.utils.logger :as log]
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
                    (q/access-project! project-id user-id)
                    (->> (repos/execute! executor))))
    (throw (ex-info "User cannot access this project" (maps/->m project-id user-id)))))

(defn ^:private access-file! [executor projects file-id user-id]
  (when (empty? (-> projects
                    (q/access-file! file-id user-id)
                    (->> (repos/execute! executor))))
    (throw (ex-info "User cannot access this file" (maps/->m file-id user-id)))))

(defn ^:private access-artifact! [executor projects artifact-id user-id]
  (when (empty? (-> projects
                    (q/access-artifact! artifact-id user-id)
                    (->> (repos/execute! executor))))
    (throw (ex-info "User cannot access this artifact" (maps/->m artifact-id user-id)))))

(defn ^:private create-version* [executor files file-versions file user-id file-id]
  (-> file-versions
      (q/insert-version file file-id user-id)
      (->> (repos/execute! executor)))
  (colls/only! (repos/execute! executor (q/select-one files file-id))))

(defn ^:private create-artifact* [executor {store :store/kv :models/keys [artifacts]} artifact user-id]
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

(defn ^:private query-for-project* [{model :models/files} project-id user-id]
  (q/select-for-user model project-id user-id))

(defn ^:private query-one-for-user* [executor {:models/keys [file-versions files projects]} file-id user-id]
  (access-file! executor projects file-id user-id)
  (let [file (colls/only! (repos/execute! executor (q/select-one-plain files file-id)))]
    (assoc file :file/versions (repos/execute! executor (q/select-versions file-versions file-id)))))

(defn ^:private create-file* [executor {:models/keys [files file-versions projects]} project-id file user-id]
  (access-project! executor projects project-id user-id)
  (-> files
      (q/insert {:name       (:file/name file)
                 :project-id project-id
                 :created-by user-id})
      (->> (repos/execute! executor))
      colls/only!
      :id
      (->> (create-version* executor files file-versions file user-id))))

(defn ^:private create-file-version* [executor {:models/keys [files file-versions projects]} file-id version user-id]
  (access-file! executor projects file-id user-id)
  (create-version* executor files file-versions version user-id file-id))

(defn ^:private get-artifact* [executor {store :store/kv :models/keys [artifacts projects]} artifact-id user-id]
  (access-artifact! executor projects artifact-id user-id)
  (let [{:artifact/keys [uri content-type]} (colls/only! (repos/execute! executor (q/select-artifact artifacts artifact-id)))]
    [(repos/get store uri)
     {:content-type content-type}]))

(deftype FileAccessor [repo]
  pint/IAccessor
  (query-many [_ opts]
    (repos/transact! repo repos/->exec! query-for-project* (:project/id opts) (:user/id opts)))
  (query-one [_ opts]
    (repos/transact! repo query-one-for-user* (:file/id opts) (:user/id opts)))

  pint/IFileAccessor
  (create-artifact! [_ opts]
    (repos/transact! repo create-artifact* opts (:user/id opts)))
  (create-file! [_ opts]
    (repos/transact! repo create-file* (:project/id opts) opts (:user/id opts)))
  (create-file-version! [_ opts]
    (repos/transact! repo create-file-version* (:file/id opts) opts (:user/id opts)))
  (get-artifact [_ opts]
    (repos/transact! repo get-artifact* (:artifact/id opts) (:user/id opts))))

(defmethod ig/init-key ::model [_ {:keys [repo]}]
  (->FileAccessor repo))
