(ns test.utils.repositories
  (:require
    [com.ben-allred.audiophile.backend.api.repositories.protocols :as prepos]
    [com.ben-allred.audiophile.backend.infrastructure.db.models.core :as models]
    [test.utils.stubs :as stubs]))

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
                  (put! [_ _ _ _]))))

(defn stub-transactor [cb]
  (stubs/create (reify
                  prepos/ITransact
                  (transact! [this f]
                    (f (cb this models)))

                  prepos/IExecute
                  (execute! [_ _ _]))))
