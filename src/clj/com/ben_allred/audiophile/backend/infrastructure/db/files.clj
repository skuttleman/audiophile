(ns com.ben-allred.audiophile.backend.infrastructure.db.files
  (:require
    [com.ben-allred.audiophile.backend.api.repositories.core :as repos]
    [com.ben-allred.audiophile.backend.api.repositories.events.core :as events]
    [com.ben-allred.audiophile.backend.api.repositories.files.protocols :as pf]
    [com.ben-allred.audiophile.backend.infrastructure.db.models.core :as models]
    [com.ben-allred.audiophile.backend.infrastructure.db.models.sql :as sql]
    [com.ben-allred.audiophile.backend.infrastructure.pubsub.ws :as ws]
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

(defn ^:private access-artifact [projects files file-versions user-teams artifact-id user-id]
  (-> projects
      (access* user-teams user-id)
      (models/join files [:= :files.project-id :projects.id])
      (models/join file-versions [:= :file-versions.file-id :files.id])
      (update :where conj [:= :file-versions.artifact-id artifact-id])
      (assoc :select [1])))

(defn ^:private access! [executor type query]
  (when (empty? (repos/execute! executor query))
    (throw (ex-info (str "User cannot access this " (name type)) query))))

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
      (models/select* [:= :files.id file-id])))

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

(defn ^:private insert-artifact [artifacts uri artifact user-id]
  (-> artifact
      (select-keys #{:content-type :filename :key})
      (assoc :uri uri
             :content-length (:size artifact)
             :created-by user-id)
      (->> (models/insert-into artifacts))))

(defn ^:private select-artifact [artifacts artifact-id]
  (models/select* artifacts [:= :artifacts.id artifact-id]))

(defn ^:private insert-version [file-versions version file-id user-id]
  (models/insert-into file-versions {:artifact-id (:artifact/id version)
                                     :file-id     file-id
                                     :name        (:version/name version)
                                     :created-by  user-id}))

(deftype FilesRepoExecutor [executor artifacts file-versions files projects user-teams store keygen]
  pf/IFilesExecutor
  (insert-file! [_ file {project-id :project/id user-id :user/id}]
    (access! executor :project (access-project projects user-teams project-id user-id))
    (let [file-id (-> files
                      (insert {:name       (:file/name file)
                               :project-id project-id
                               :created-by user-id})
                      (->> (repos/execute! executor))
                      colls/only!
                      :id)]
      (-> file-versions
          (insert-version file file-id user-id)
          (->> (repos/execute! executor)))
      file-id))
  (insert-version! [_ version opts]
    (access! executor :file (access-file projects files user-teams (:file/id opts) (:user/id opts)))
    (-> file-versions
        (insert-version version (:file/id opts) (:user/id opts))
        (->> (repos/execute! executor))
        colls/only!
        :id))
  (insert-artifact! [_ artifact opts]
    (let [key (keygen)]
      (with-async
        (repos/put! store
                    key
                    (:tempfile artifact)
                    {:content-type   (:content-type artifact)
                     :content-length (:size artifact)
                     :metadata       {:filename (:filename artifact)}})
        (->> opts
             :user/id
             (insert-artifact artifacts (repos/uri store key) (assoc artifact :key key))
             (repos/execute! executor)
             colls/only!
             :id))))
  (find-by-file-id [_ file-id opts]
    (when-not (:internal/verified? opts)
      (access! executor :file (access-file projects files user-teams file-id (:user/id opts))))
    (when-let [file (-> executor
                        (repos/execute! (if (:includes/versions? opts)
                                          (select-one-plain files file-id)
                                          (select-one files file-id)))
                        colls/only!)]
      (cond-> file
        (:includes/versions? opts)
        (assoc :file/versions (repos/execute! executor
                                              (select-versions file-versions file-id))))))
  (find-by-artifact-id [_ artifact-id opts]
    (access! executor :artifact (access-artifact projects files file-versions user-teams artifact-id (:user/id opts)))
    (let [artifact (-> executor
                       (repos/execute! (select-artifact artifacts artifact-id))
                       colls/only!)]
      (assoc artifact :artifact/data (repos/get store (:artifact/key artifact)))))
  (find-event-artifact [_ artifact-id]
    (-> executor
        (repos/execute! (select-artifact artifacts artifact-id))
        colls/only!))
  (select-for-project [_ project-id opts]
    (repos/execute! executor (select-for-user* files
                                               projects
                                               user-teams
                                               project-id
                                               (:user/id opts)))))

(defn ->file-executor [{:keys [artifacts file-versions files projects user-teams store]}]
  (fn [executor]
    (->FilesRepoExecutor executor
                         artifacts
                         file-versions
                         files
                         projects
                         user-teams
                         store
                         #(str "artifacts/" (uuids/random)))))

(deftype FilesEventEmitter [executor pubsub]
  pf/IFilesEventEmitter
  (artifact-created! [_ user-id {:artifact/keys [id] :as artifact}]
    (let [event {:event/model-id id
                 :event/data     artifact}
          event-id (events/insert-event! executor
                                         event
                                         {:event/type :artifact/created
                                          :user/id    user-id})]
      (ws/send-user! pubsub user-id event-id (assoc event
                                                    :event/id event-id
                                                    :event/type :artifact/created
                                                    :event/emitted-by user-id))
      event-id)))

(defn ->file-event-emitter [{:keys [pubsub]}]
  (fn [executor]
    (->FilesEventEmitter executor pubsub)))

(deftype Executor [executor emitter]
  pf/IFilesExecutor
  (insert-file! [_ file opts]
    (pf/insert-file! executor file opts))
  (insert-version! [_ version opts]
    (pf/insert-version! executor version opts))
  (insert-artifact! [_ artifact opts]
    (pf/insert-artifact! executor artifact opts))
  (find-by-file-id [_ file-id opts]
    (pf/find-by-file-id executor file-id opts))
  (find-by-artifact-id [_ artifact-id opts]
    (pf/find-by-artifact-id executor artifact-id opts))
  (find-event-artifact [_ artifact-id]
    (pf/find-event-artifact executor artifact-id))
  (select-for-project [_ project-id opts]
    (pf/select-for-project executor project-id opts))

  pf/IFilesEventEmitter
  (artifact-created! [_ user-id opts]
    (pf/artifact-created! emitter user-id opts)))

(defn ->executor [{:keys [->event-executor ->file-event-emitter ->file-executor]}]
  (fn [executor]
    (->Executor (->file-executor executor)
                (->file-event-emitter (->event-executor executor)))))
