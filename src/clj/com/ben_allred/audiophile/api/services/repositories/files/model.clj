(ns com.ben-allred.audiophile.api.services.repositories.files.model
  (:require
    [com.ben-allred.audiophile.api.services.repositories.core :as repos]
    [com.ben-allred.audiophile.api.services.repositories.entities.core :as entities]
    [com.ben-allred.audiophile.api.services.repositories.files.queries :as qfiles]
    [com.ben-allred.audiophile.common.utils.colls :as colls]
    [com.ben-allred.audiophile.common.utils.fns :as fns]
    [com.ben-allred.audiophile.common.utils.maps :as maps]
    [com.ben-allred.audiophile.common.utils.uuids :as uuids]))

(defmacro ^:private with-unexceptional [fut & body]
  `(let [future# (future ~fut)]
     (try (let [result# (do ~@body)]
            @future#
            result#)
          (catch Throwable ex#
            (future-cancel future#)
            (throw ex#)))))

(defn ^:private access! [entity user-id]
  (-> entity
      (entities/select-fields #{:id})
      (entities/select* [:and
                         [:= :user-teams.user-id user-id]])
      (entities/join {:table :user-teams}
                     [:= :user-teams.team-id :projects.team-id])))

(defn ^:private access-project! [executor projects project-id user-id]
  (when (empty? (-> projects
                    (access! user-id)
                    (update :where conj [:= :projects-id project-id])
                    (->> (repos/execute! executor))))
    (throw (ex-info "User cannot access this project" (maps/->m project-id user-id)))))

(defn ^:private access-file! [executor projects file-id user-id]
  (when (empty? (-> projects
                    (access! user-id)
                    (entities/join {:table :files} [:= :files.project-id :projects.id])
                    (update :where conj [:= :files.id file-id])
                    (->> (repos/execute! executor))))
    (throw (ex-info "User cannot access this file" (maps/->m file-id user-id)))))

(defn ^:private create-version* [executor files file-versions file user-id file-id]
  (-> file-versions
      (entities/insert-into {:artifact-id (:artifact/id file)
                             :file-id     file-id
                             :name        (:version/name file)
                             :created-by  user-id})
      (->> (repos/execute! executor)))
  (repos/execute! executor (qfiles/select-one files file-id)))

(defn create-artifact
  "Save an artifact to the repository and upload the content to the kv store.
   If write to kv store fails, repository will be rolled back. Otherwise, cleanup TBD"
  [repo artifact user-id]
  (repos/transact! repo (fn [executor {store :store/kv :entity/keys [artifacts]}]
                          (let [key (str "artifacts/" (uuids/random))]
                            (with-unexceptional
                              (repos/put! store
                                          key
                                          (:tempfile artifact)
                                          {:content-type (:content-type artifact)
                                           :metadata     {:filename (:filename artifact)}})
                              {:artifact/id       (-> artifact
                                                      (select-keys #{:content-type :filename})
                                                      (assoc :uri (repos/uri store key)
                                                             :content-size (:size artifact)
                                                             :created-by user-id)
                                                      (->> (entities/insert-into artifacts))
                                                      (->> (repos/execute! executor))
                                                      colls/only!
                                                      :id)
                               :artifact/filename (:filename artifact)})))))

(defn query-many
  "Query files for a project"
  [repo project-id user-id]
  (repos/transact! repo
                   repos/->exec!
                   (fns/=> :entity/files
                           (qfiles/select-for-user project-id user-id))))

(defn create-file
  "Save a new file with a version to the repository."
  [repo project-id file user-id]
  (repos/transact! repo
                   (fn [executor {:entity/keys [files file-versions projects]}]
                     (access-project! executor projects project-id user-id)
                     (-> files
                         (qfiles/insert {:name       (:file/name file)
                                         :project-id project-id
                                         :created-by user-id})
                         (->> (repos/execute! executor))
                         colls/only!
                         :id
                         (->> (create-version* executor
                                               files
                                               file-versions
                                               file
                                               user-id))))))

(defn create-file-version
  "Create a new version of an existing file"
  [repo file-id version user-id]
  (repos/transact! repo
                   (fn [executor {:entity/keys [files file-versions projects]}]
                     (access-file! executor projects file-id user-id)
                     (->> (create-version* executor
                                           files
                                           file-versions
                                           version
                                           user-id
                                           file-id)))))
