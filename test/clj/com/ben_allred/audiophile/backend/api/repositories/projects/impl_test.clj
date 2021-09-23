(ns ^:unit com.ben-allred.audiophile.backend.api.repositories.projects.impl-test
  (:require
    [clojure.test :refer [are deftest is testing]]
    [com.ben-allred.audiophile.backend.api.repositories.projects.impl :as rprojects]
    [com.ben-allred.audiophile.backend.domain.interactors.core :as int]
    [com.ben-allred.audiophile.backend.infrastructure.db.projects :as db.projects]
    [com.ben-allred.audiophile.backend.infrastructure.pubsub.core :as ps]
    [com.ben-allred.audiophile.common.core.utils.colls :as colls]
    [com.ben-allred.audiophile.common.core.utils.fns :as fns]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.common.core.utils.uuids :as uuids]
    [com.ben-allred.audiophile.common.infrastructure.pubsub.protocols :as ppubsub]
    [test.utils :as tu]
    [test.utils.repositories :as trepos]
    [test.utils.stubs :as stubs]))

(defn ^:private ->project-executor
  ([config]
   (fn [executor models]
     (->project-executor executor (merge models config))))
  ([executor {:keys [projects pubsub teams user-teams users]}]
   (let [project-exec (db.projects/->ProjectsRepoExecutor executor
                                                          projects
                                                          teams
                                                          user-teams
                                                          users)]
     (db.projects/->Executor project-exec pubsub))))

(deftest query-all-test
  (testing "query-all"
    (let [tx (trepos/stub-transactor ->project-executor)
          repo (rprojects/->ProjectAccessor tx)
          user-id (uuids/random)]
      (testing "when querying for projects"
        (stubs/use! tx :execute!
                    [{:some :result}])
        (let [result (int/query-many repo {:user/id user-id})
              [{:keys [select from where]}] (colls/only! (stubs/calls tx :execute!))]
          (testing "sends a query to the repository"
            (is (= #{[:projects.team-id "project/team-id"]
                     [:projects.name "project/name"]
                     [:projects.id "project/id"]
                     [:projects.created-at "project/created-at"]}
                   (set select)))
            (is (= [:projects] from))
            (is (= [:exists {:select #{1}
                             :from   [:user-teams]
                             :where  [:and
                                      #{[:= #{:projects.team-id :user-teams.team-id}]
                                        [:= #{:user-teams.user-id user-id}]}]}]
                   (-> where
                       (update-in [1 :select] set)
                       (update-in [1 :where 1] tu/op-set)
                       (update-in [1 :where 2] tu/op-set)
                       (update-in [1 :where] tu/op-set)))))

          (testing "returns the results"
            (is (= [{:some :result}] result))))))))

(deftest query-by-id-test
  (testing "query-by-id"
    (let [tx (trepos/stub-transactor ->project-executor)
          repo (rprojects/->ProjectAccessor tx)
          [project-id user-id] (repeatedly uuids/random)]
      (testing "when querying for a single project"
        (stubs/use! tx :execute!
                    [{:some :result}])

        (let [result (int/query-one repo {:user/id    user-id
                                          :project/id project-id})]
          (testing "sends a query to the repository"
            (let [[{:keys [from select where]}] (colls/only! (stubs/calls tx :execute!))]
              (is (= [:projects] from))
              (is (= #{[:projects.team-id "project/team-id"]
                       [:projects.name "project/name"]
                       [:projects.id "project/id"]
                       [:projects.created-at "project/created-at"]}
                     (set select)))
              (let [[clause clauses] (tu/op-set where)
                    clauses' (into {} (map (juxt tu/op-set identity)) clauses)]
                (is (= :and clause))
                (is (contains? clauses'
                               [:= #{:projects.id project-id}]))
                (is (= [:exists {:select #{1}
                                 :from   [:user-teams]
                                 :where  [:and
                                          #{[:= #{:projects.team-id :user-teams.team-id}]
                                            [:= #{:user-teams.user-id user-id}]}]}]
                       (-> clauses'
                           (dissoc [:= #{:projects.id project-id}])
                           colls/only!
                           val
                           (update 1 (fns/=> (update :select set)
                                             (update :where (fns/=> (update 1 tu/op-set)
                                                                    (update 2 tu/op-set)
                                                                    tu/op-set))))))))))

          (testing "returns the results"
            (is (= {:some :result} result))))))))

(deftest create!-test
  (testing "create!"
    (let [pubsub (stubs/create (reify
                                 ppubsub/IPub
                                 (publish! [_ _ _])
                                 ppubsub/ISub
                                 (subscribe! [_ _ _ _])
                                 (unsubscribe! [_ _])
                                 (unsubscribe! [_ _ _])))
          tx (trepos/stub-transactor (->project-executor {:pubsub pubsub}))
          repo (rprojects/->ProjectAccessor tx)
          handler (rprojects/command-handler {:accessor repo
                                              :pubsub   pubsub})
          [project-id team-id user-id] (repeatedly uuids/random)
          project {:project/id      project-id
                   :project/name    "some project"
                   :project/team-id team-id}]
      (testing "when creating a project"
        (stubs/use! tx :execute!
                    [{:id "team-id"}]
                    [{:id project-id}]
                    [project]
                    [{:id "event-id"}])
        (handler {:msg [::id
                        {:command/type :project/create!
                         :command/data {:created-at      :whenever
                                        :project/team-id team-id
                                        :other           :junk}}
                        {:user/id user-id}]})

        (let [[[access] [insert] [query-for-event]] (colls/only! 3 (stubs/calls tx :execute!))]
          (testing "verifies team access"
            (is (= {:select [1]
                    :from   [:teams]
                    :where  [:and
                             #{[:= #{:teams.id team-id}]
                               [:= #{:user-teams.user-id user-id}]}]
                    :join   [:user-teams [:= :user-teams.team-id :teams.id]]}
                   (-> access
                       (update-in [:where 1] tu/op-set)
                       (update-in [:where 2] tu/op-set)
                       (update :where tu/op-set)))))

          (testing "saves to the repository"
            (is (= {:insert-into :projects
                    :values      [{:created-at :whenever
                                   :team-id    team-id}]
                    :returning   [:id]}
                   insert)))

          (testing "queries from the repository"
            (is (= {:select #{[:projects.team-id "project/team-id"]
                              [:projects.name "project/name"]
                              [:projects.id "project/id"]
                              [:projects.created-at "project/created-at"]}
                    :from   [:projects]
                    :where  [:= #{:projects.id project-id}]}
                   (-> query-for-event
                       (select-keys #{:select :from :where})
                       (update :select set)
                       (update :where tu/op-set))))))

        (testing "emits an event"
          (let [[topic [event-id event]] (colls/only! (stubs/calls pubsub :publish!))]
            (is (= [::ps/user user-id] topic))
            (is (uuid? event-id))
            (is (= {:event/id         event-id
                    :event/type       :project/created
                    :event/model-id   project-id
                    :event/data       project
                    :event/emitted-by user-id}
                   event)))))

      (testing "when the executor throws an exception"
        (let [request-id (uuids/random)
              user-id (uuids/random)]
          (stubs/init! pubsub)
          (stubs/use! tx :execute!
                      (ex-info "Executor" {}))
          (handler {:msg [::id
                          {:command/type :project/create!
                           :command/data {}}
                          {:user/id    user-id
                           :request/id request-id}]})

          (testing "does not emit a successful event"
            (empty? (stubs/calls pubsub :publish!)))

          (testing "emits a command-failed event"
            (let [[topic [event-id event ctx]] (-> pubsub
                                                   (stubs/calls :publish!)
                                                   colls/only!)]
              (is (= [::ps/user user-id] topic))
              (is (uuid? event-id))
              (is (= {:event/id         event-id
                      :event/model-id   request-id
                      :event/type       :command/failed
                      :event/data       {:error/command :project/create!
                                         :error/reason  "Executor"}
                      :event/emitted-by user-id}
                     event))
              (is (= {:request/id request-id
                      :user/id    user-id}
                     ctx))))))

      (testing "when the pubsub throws an exception"
        (let [request-id (uuids/random)
              user-id (uuids/random)]
          (stubs/init! pubsub)
          (stubs/use! tx :execute!
                      [{:id "project-id"}])
          (stubs/use! pubsub :publish!
                      (ex-info "Pubsub" {}))
          (handler {:msg [::id
                          {:command/type :project/create!
                           :command/data {}}
                          {:user/id    user-id
                           :request/id request-id}]})

          (testing "emits a command-failed event"
            (let [[topic [event-id event ctx]] (-> pubsub
                                                   (stubs/calls :publish!)
                                                   rest
                                                   colls/only!)]
              (is (= [::ps/user user-id] topic))
              (is (uuid? event-id))
              (is (= {:event/id         event-id
                      :event/model-id   request-id
                      :event/type       :command/failed
                      :event/data       {:error/command :project/create!
                                         :error/reason  "Pubsub"}
                      :event/emitted-by user-id}
                     event))
              (is (= {:request/id request-id
                      :user/id    user-id}
                     ctx)))))))))
