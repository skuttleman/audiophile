(ns com.ben-allred.audiophile.backend.infrastructure.db.files
  (:require
    [com.ben-allred.audiophile.backend.api.repositories.core :as repos]
    [com.ben-allred.audiophile.backend.api.repositories.files.protocols :as pf]
    [com.ben-allred.audiophile.backend.infrastructure.db.common :as cdb]
    [com.ben-allred.audiophile.backend.infrastructure.db.models.core :as models]
    [com.ben-allred.audiophile.backend.infrastructure.db.models.sql :as sql]
    [com.ben-allred.audiophile.common.core.utils.colls :as colls]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]))

(defn ^:private has-team-clause [projects user-teams project-id user-id]
  [:and
   [:= :files.project-id project-id]
   [:exists (-> projects
                (models/select* [:and
                                 [:= :projects.id :files.project-id]
                                 [:= :user-teams.user-id user-id]])
                (models/join user-teams [:= :user-teams.team-id :projects.team-id])
                (assoc :select [1]))]])

(defn ^:private access* [projects user-teams user-id]
  (-> projects
      (models/select* [:and [:= :user-teams.user-id user-id]])
      (models/join user-teams [:= :user-teams.team-id :projects.team-id])))

(defn ^:private access-project [projects user-teams project-id user-id]
  (-> projects
      (access* user-teams user-id)
      (update :where conj [:= :projects.id project-id])
      (assoc :select [1])))

(defn ^:private access-file [projects files user-teams file-id user-id]
  (-> projects
      (access* user-teams user-id)
      (models/join files [:= :files.project-id :projects.id])
      (update :where conj [:= :files.id file-id])
      (assoc :select [1])))

(defn ^:private file-access-clause [projects user-teams user-id]
  (-> projects
      (access* user-teams user-id)
      (update :where conj [:= :projects.id :files.project-id])
      (assoc :select [1])))

(defn ^:private artifact-access-clause [projects files file-versions user-teams user-id]
  (-> projects
      (access* user-teams user-id)
      (models/join files [:= :files.project-id :projects.id])
      (models/join file-versions [:= :file-versions.file-id :files.id])
      (update :where conj [:= :file-versions.artifact-id :artifacts.id])
      (assoc :select [1])))

(defn ^:private select-by [files clause]
  (-> files
      (models/select-fields #{:id :idx :name :project-id})
      (models/select* clause)
      (models/join {:table  {:select   [:file-versions.file-id
                                        [(sql/max :file-versions.created-at) :created-at]]
                             :from     [:file-versions]
                             :group-by [:file-versions.file-id]}
                    :fields #{:created-at}
                    :alias  :version}
                   [:= :version.file-id :files.id])
      (models/join {:table     :file-versions
                    :namespace :version
                    :fields    #{}
                    :alias     :fv}
                   [:and
                    [:= :fv.file-id :version.file-id]
                    [:= :fv.created-at :version.created-at]])
      (update :select into [[:fv.id "version/id"]
                            [:fv.name "version/name"]
                            [:fv.artifact-id "version/artifact-id"]])))

(defn ^:private select-one [files file-id]
  (select-by files [:= :files.id file-id]))

(defn ^:private select-for-user* [files projects user-teams project-id user-id]
  (-> files
      (select-by (has-team-clause projects user-teams project-id user-id))
      (models/order-by [:files.idx :asc]
                       [:version.created-at :desc])))

(defn ^:private select-one-plain [files file-id]
  (-> files
      (models/select-fields #{:id :idx :name :project-id})
      (models/select-by-id* file-id)))

(defn ^:private select-versions [file-versions file-id]
  (-> file-versions
      (models/select-fields #{:id :name :artifact-id})
      (models/select* [:= :file-versions.file-id file-id])
      (models/order-by [:file-versions.created-at :desc])))

(defn ^:private insert [files file]
  (models/insert-into files
                      (assoc file
                             :idx (-> files
                                      (models/select* [:= :files.project-id (:project-id file)])
                                      (assoc :select [(sql/count :idx)])))))

(defn ^:private insert-artifact [artifacts artifact]
  (-> artifact
      (select-keys #{:artifact/content-type :artifact/filename :artifact/key :artifact/uri})
      (assoc :artifact/content-length (:artifact/size artifact))
      (->> (models/insert-into artifacts))))

(defn ^:private insert-version [file-versions version file-id]
  (models/insert-into file-versions {:artifact-id (:artifact/id version)
                                     :file-id     file-id
                                     :name        (:version/name version)}))

(deftype FilesRepoExecutor [executor artifacts file-versions files projects user-teams]
  pf/IArtifactsExecutor
  (insert-artifact-access? [_ _ _]
    true)
  (insert-artifact! [_ artifact _]
    (->> artifact
         (insert-artifact artifacts)
         (repos/execute! executor)
         colls/only!
         :id))
  (find-by-artifact-id [_ artifact-id opts]
    (-> artifacts
        models/select*
        (assoc :where [:and
                       [:= :artifacts.id artifact-id]
                       [:exists (artifact-access-clause projects
                                                        files
                                                        file-versions
                                                        user-teams
                                                        (:user/id opts))]])
        (->> (repos/execute! executor))
        colls/only!))
  (find-event-artifact [_ artifact-id]
    (-> executor
        (repos/execute! (models/select-by-id* artifacts artifact-id))
        colls/only!))

  pf/IFilesExecutor
  (insert-file-access? [_ _ {project-id :project/id user-id :user/id}]
    (cdb/access? executor (access-project projects user-teams project-id user-id)))
  (insert-file! [_ file {project-id :project/id}]
    (let [file-id (-> files
                      (insert {:name       (:file/name file)
                               :project-id project-id})
                      (->> (repos/execute! executor))
                      colls/only!
                      :id)]
      (-> file-versions
          (insert-version file file-id)
          (->> (repos/execute! executor)))
      file-id))
  (find-by-file-id [_ file-id opts]
    (when-let [file (-> (if (:includes/versions? opts)
                          (select-one-plain files file-id)
                          (select-one files file-id))
                        (cond->
                          (not (:internal/verified? opts))
                          (update :where (fn [clause]
                                           [:and
                                            clause
                                            [:exists (file-access-clause projects
                                                                         user-teams
                                                                         (:user/id opts))]])))
                        (->> (repos/execute! executor))
                        colls/only!)]
      (cond-> file
        (:includes/versions? opts)
        (assoc :file/versions (repos/execute! executor
                                              (select-versions file-versions file-id))))))
  (select-for-project [_ project-id opts]
    (repos/execute! executor (select-for-user* files
                                               projects
                                               user-teams
                                               project-id
                                               (:user/id opts))))
  (find-event-file [_ file-id]
    (-> executor
        (repos/execute! (select-one files file-id))
        colls/only!))

  pf/IFileVersionsExecutor
  (insert-version-access? [_ _ {file-id :file/id user-id :user/id}]
    (cdb/access? executor (access-file projects files user-teams file-id user-id)))
  (insert-version! [_ version {file-id :file/id}]
    (-> file-versions
        (insert-version version file-id)
        (->> (repos/execute! executor))
        colls/only!
        :id))
  (find-event-version [_ version-id]
    (-> executor
        (repos/execute! (models/select-by-id* file-versions version-id))
        colls/only!)))

(defn ->file-executor
  "Factory function for creating [[FilesRepoExecutor]] which provide access to the file repository."
  [{:keys [artifacts file-versions files projects user-teams]}]
  (fn [executor]
    (->FilesRepoExecutor executor
                         artifacts
                         file-versions
                         files
                         projects
                         user-teams)))
