(ns com.ben-allred.audiophile.backend.infrastructure.db.files
  (:require
    [com.ben-allred.audiophile.backend.api.repositories.core :as repos]
    [com.ben-allred.audiophile.backend.api.repositories.files.protocols :as pf]
    [com.ben-allred.audiophile.backend.domain.interactors.protocols :as pint]
    [com.ben-allred.audiophile.backend.infrastructure.db.common :as cdb]
    [com.ben-allred.audiophile.backend.infrastructure.db.models.core :as models]
    [com.ben-allred.audiophile.backend.infrastructure.db.models.sql :as sql]
    [com.ben-allred.audiophile.common.core.utils.colls :as colls]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.common.core.utils.uuids :as uuids]))

(defmacro ^:private with-async [fut & body]
  `(let [future# (future ~fut)]
     (try (let [result# (do ~@body)]
            @future#
            result#)
          (catch Throwable ex#
            (future-cancel future#)
            (throw ex#)))))

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

(defn ^:private insert-artifact [artifacts uri artifact]
  (-> artifact
      (select-keys #{:content-type :filename :key})
      (assoc :uri uri
             :content-length (:size artifact))
      (->> (models/insert-into artifacts))))

(defn ^:private insert-version [file-versions version file-id]
  (models/insert-into file-versions {:artifact-id (:artifact/id version)
                                     :file-id     file-id
                                     :name        (:version/name version)}))

(deftype FilesRepoExecutor [executor artifacts file-versions files projects user-teams store keygen]
  pf/IArtifactsExecutor
  (insert-artifact-access? [_ _ _]
    true)
  (insert-artifact! [_ artifact _]
    (let [key (keygen)]
      (with-async
        (repos/put! store
                    key
                    (:tempfile artifact)
                    {:content-type   (:content-type artifact)
                     :content-length (:size artifact)
                     :metadata       {:filename (:filename artifact)}})
        (->> (assoc artifact :key key)
             (insert-artifact artifacts (repos/uri store key))
             (repos/execute! executor)
             colls/only!
             :id))))
  (find-by-artifact-id [_ artifact-id opts]
    (when-let [artifact (-> artifacts
                            models/select*
                            (assoc :where [:and
                                           [:= :artifacts.id artifact-id]
                                           [:exists (artifact-access-clause projects
                                                                            files
                                                                            file-versions
                                                                            user-teams
                                                                            (:user/id opts))]])
                            (->> (repos/execute! executor))
                            colls/only!)]
      (assoc artifact :artifact/data (repos/get store (:artifact/key artifact)))))
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
  [{:keys [artifacts file-versions files projects user-teams store]}]
  (fn [executor]
    (->FilesRepoExecutor executor
                         artifacts
                         file-versions
                         files
                         projects
                         user-teams
                         store
                         #(str "artifacts/" (uuids/random)))))

(deftype Executor [executor pubsub]
  pf/IArtifactsExecutor
  (insert-artifact-access? [_ artifact opts]
    (pf/insert-artifact-access? executor artifact opts))
  (insert-artifact! [_ artifact opts]
    (pf/insert-artifact! executor artifact opts))
  (find-by-artifact-id [_ artifact-id opts]
    (pf/find-by-artifact-id executor artifact-id opts))
  (find-event-artifact [_ artifact-id]
    (pf/find-event-artifact executor artifact-id))

  pf/IFilesExecutor
  (insert-file-access? [_ file opts]
    (pf/insert-file-access? executor file opts))
  (insert-file! [_ file opts]
    (pf/insert-file! executor file opts))
  (find-by-file-id [_ file-id opts]
    (pf/find-by-file-id executor file-id opts))
  (select-for-project [_ project-id opts]
    (pf/select-for-project executor project-id opts))
  (find-event-file [_ file-id]
    (pf/find-event-file executor file-id))

  pf/IFileVersionsExecutor
  (insert-version-access? [_ version opts]
    (pf/insert-version-access? executor version opts))
  (insert-version! [_ version opts]
    (pf/insert-version! executor version opts))
  (find-event-version [_ version-id]
    (pf/find-event-version executor version-id))

  pf/IArtifactsEventEmitter
  (artifact-created! [_ user-id artifact ctx]
    (cdb/emit! pubsub user-id (:artifact/id artifact) :artifact/created artifact ctx))

  pf/IFilesEventEmitter
  (file-created! [_ user-id file ctx]
    (cdb/emit! pubsub user-id (:file/id file) :file/created file ctx))

  pf/IFileVersionsEventEmitter
  (version-created! [_ user-id version ctx]
    (cdb/emit! pubsub user-id (:file-version/id version) :file-version/created version ctx))

  pint/IEmitter
  (command-failed! [_ model-id opts]
    (cdb/command-failed! pubsub model-id opts)))

(defn ->executor
  "Factory function for creating [[Executor]] which aggregates [[FilesEventEmitter]]
   and [[FilesRepoExecutor]]."
  [{:keys [->file-executor pubsub]}]
  (fn [executor]
    (->Executor (->file-executor executor) pubsub)))
