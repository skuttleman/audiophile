(ns audiophile.test.utils.repositories
  (:require
    [audiophile.backend.api.repositories.protocols :as prepos]
    [audiophile.backend.infrastructure.db.comments :as db.comments]
    [audiophile.backend.infrastructure.db.files :as db.files]
    [audiophile.backend.infrastructure.db.models.core :as models]
    [audiophile.backend.infrastructure.db.projects :as db.projects]
    [audiophile.backend.infrastructure.db.teams :as db.teams]
    [audiophile.backend.infrastructure.db.users :as db.users]
    [audiophile.test.utils.stubs :as stubs]))

(defn ^:private ->model [[table-name {:keys [spec namespace]}]]
  [table-name (models/model {:models     {table-name {:fields (set (keys spec))
                                                      :spec   spec}}
                             :table-name table-name
                             :namespace  namespace})])

(def models
  (into {}
        (map ->model)
        {:artifacts     {:spec      {:id           [:uuid]
                                     :filename     [:text]
                                     :content-type [:text]
                                     :uri          [:text]
                                     :content-size [:text]
                                     :created-at   [:timestamp-without-time-zone]}
                         :namespace :artifact}
         :comments      {:spec      {:id              [:uuid]
                                     :body            [:text]
                                     :file-version-id [:uuid]
                                     :selection       [:numrange true]
                                     :comment-id      [:uuid true]
                                     :created-at      [:timestamp-without-time-zone]}
                         :namespace :comment}
         :events        {:spec      {:id            [:uuid]
                                     :data          [:jsonb true]
                                     :event-type-id [:uuid]
                                     :model-id      [:uuid]
                                     :emitted-at    [:timestamp-without-time-zone]
                                     :emitted-by    [:uuid]}
                         :namespace :event}
         :event-types   {:spec      {:id       [:uuid]
                                     :category [:text]
                                     :name     [:text]}
                         :namespace :event-type}
         :files         {:spec      {:name       [:text]
                                     :id         [:uuid]
                                     :idx        [:integer]
                                     :project-id [:uuid]
                                     :created-at [:timestamp-without-time-zone]}
                         :namespace :file}
         :file-versions {:spec      {:file-id     [:uuid]
                                     :artifact-id [:uuid]
                                     :name        [:text]
                                     :id          [:uuid]
                                     :created-at  [:timestamp-without-time-zone]}
                         :namespace :file-version}
         :projects      {:spec      {:team-id    [:uuid]
                                     :name       [:text]
                                     :id         [:uuid]
                                     :created-at [:timestamp-without-time-zone]}
                         :namespace :project}
         :teams         {:spec      {:type       [:user-defined]
                                     :name       [:text]
                                     :id         [:uuid]
                                     :created-at [:timestamp-without-time-zone]}
                         :namespace :team}
         :user-teams    {:spec      {:team-id [:uuid]
                                     :user-id [:uuid]}
                         :namespace :user-team}
         :users         {:spec      {:id            [:uuid]
                                     :first-name    [:text]
                                     :last-name     [:text]
                                     :handle        [:text]
                                     :email         [:text]
                                     :mobile-number [:text]
                                     :created-at    [:timestamp-without-time-zone]}
                         :namespace :user}}))

(defn stub-kv-store []
  (stubs/create (reify
                  prepos/IKVStore
                  (uri [_ key _]
                    (str "test://uri/" key))
                  (get [_ _ _])
                  (put! [_ _ _ _]))))

(defn stub-transactor [cb]
  (stubs/create (reify
                  prepos/ITransact
                  (transact! [this f]
                    (f (cb this models)))

                  prepos/IExecute
                  (execute! [_ _ _]))))

(defn ->comment-executor
  ([config]
   (fn [executor models]
     (->comment-executor executor (merge models config))))
  ([executor {:keys [comments file-versions files projects user-teams users]}]
   (db.comments/->CommentsRepoExecutor executor
                                       comments
                                       projects
                                       files
                                       file-versions
                                       user-teams
                                       users)))

(defn ->file-executor
  ([config]
   (fn [executor models]
     (->file-executor executor (merge models config))))
  ([executor {:keys [artifacts file-versions files projects user-teams]}]
   (db.files/->FilesRepoExecutor executor
                                 artifacts
                                 file-versions
                                 files
                                 projects
                                 user-teams)))

(defn ->project-executor
  ([config]
   (fn [executor models]
     (->project-executor executor (merge models config))))
  ([executor {:keys [projects teams user-teams users]}]
   (db.projects/->ProjectsRepoExecutor executor
                                       projects
                                       teams
                                       user-teams
                                       users)))

(defn ->team-executor
  ([config]
   (fn [executor models]
     (->team-executor executor (merge models config))))
  ([executor {:keys [teams user-teams users]}]
   (db.teams/->TeamsRepoExecutor executor
                                 teams
                                 user-teams
                                 users)))

(defn ->user-executor [executor {:keys [user-teams users]}]
  (db.users/->UserExecutor executor users user-teams))
