(ns test.utils.repositories
  (:require
    [com.ben-allred.audiophile.backend.api.repositories.protocols :as prepos]
    [test.utils.stubs :as stubs]))

(def models
  {:artifacts
   {:fields    #{:filename :created-by :id :content-type :uri :content-size :created-at}
    :table     :artifacts
    :namespace :artifact}

   :events
   {:fields    #{:id :data :event-type-id :model-id :emitted-at :emitted-by}
    :table     :events
    :namespace :event}

   :event-types
   {:fields    #{:id :category :name}
    :table     :event-types
    :namespace :event-type}

   :files
   {:fields    #{:name :created-by :id :idx :project-id :created-at}
    :table     :files
    :namespace :file}

   :file-versions
   {:fields    #{:file-id :artifact-id :name :created-by :id :created-at}
    :table     :file-versions
    :namespace :file-version}

   :projects
   {:fields    #{:team-id :name :created-by :id :created-at}
    :table     :projects
    :namespace :project}

   :teams
   {:fields    #{:type :name :created-by :id :created-at}
    :table     :teams
    :namespace :team}

   :user-teams
   {:fields    #{:team-id :user-id}
    :table     :user-teams
    :namespace :user-team}

   :users
   {:fields    #{:id :first-name :last-name :handle :email :mobile-number :created-at}
    :table     :users
    :namespace :user}})

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
