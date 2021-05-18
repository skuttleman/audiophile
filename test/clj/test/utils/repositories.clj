(ns test.utils.repositories
  (:require
    [com.ben-allred.audiophile.api.services.repositories.protocols :as prepos]
    [test.utils.stubs :as stubs]))

(def ^:private opts
  {:models/artifacts
   {:fields    #{:filename :created-by :id :content-type :uri :content-size :created-at}
    :table     :artifacts
    :namespace :artifact}

   :models/files
   {:fields    #{:name :created-by :id :idx :project-id :created-at}
    :table     :files
    :namespace :file}

   :models/file-versions
   {:fields    #{:file-id :artifact-id :name :created-by :id :created-at}
    :table     :file-versions
    :namespace :file-version}

   :models/projects
   {:fields    #{:team-id :name :created-by :id :created-at}
    :table     :projects
    :namespace :project}

   :models/teams
   {:fields    #{:type :name :created-by :id :created-at}
    :table     :teams
    :namespace :team}

   :models/user-teams
   {:fields    #{:team-id :user-id}
    :table     :user-teams
    :namespace :user-team}

   :models/users
   {:fields    #{:id :first-name :last-name :handle :email :mobile-number :created-at}
    :table     :users
    :namespace :user}})

(defn stub-kv-store []
  (stubs/create (reify
                  prepos/IKVStore
                  (uri [_ key _]
                    (str "test://uri/" key))
                  (put! [_ _ _ _]))))

(defn stub-transactor
  ([]
   (stub-transactor nil))
  ([kv-store]
   (stubs/create (reify
                   prepos/ITransact
                   (transact! [this f]
                     (f this (assoc opts :store/kv kv-store)))

                   prepos/IExecute
                   (execute! [_ _ _])))))
