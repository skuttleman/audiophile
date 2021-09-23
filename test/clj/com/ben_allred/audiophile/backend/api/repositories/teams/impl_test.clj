(ns ^:unit com.ben-allred.audiophile.backend.api.repositories.teams.impl-test
  (:require
    [clojure.test :refer [are deftest is testing]]
    [com.ben-allred.audiophile.backend.api.repositories.teams.impl :as rteams]
    [com.ben-allred.audiophile.backend.domain.interactors.core :as int]
    [com.ben-allred.audiophile.backend.infrastructure.db.teams :as db.teams]
    [com.ben-allred.audiophile.backend.infrastructure.pubsub.core :as ps]
    [com.ben-allred.audiophile.common.core.utils.colls :as colls]
    [com.ben-allred.audiophile.common.core.utils.fns :as fns]
    [com.ben-allred.audiophile.common.core.utils.uuids :as uuids]
    [com.ben-allred.audiophile.common.infrastructure.pubsub.protocols :as ppubsub]
    [test.utils :as tu]
    [test.utils.repositories :as trepos]
    [test.utils.stubs :as stubs]))

(defn ^:private ->team-executor
  ([config]
   (fn [executor models]
     (->team-executor executor (merge models config))))
  ([executor {:keys [teams pubsub user-teams users]}]
   (let [team-exec (db.teams/->TeamsRepoExecutor executor
                                                 teams
                                                 user-teams
                                                 users)]
     (db.teams/->Executor team-exec pubsub))))

(deftest query-all-test
  (testing "query-all"
    (let [tx (trepos/stub-transactor ->team-executor)
          repo (rteams/->TeamAccessor tx)
          user-id (uuids/random)]
      (testing "when querying teams"
        (stubs/use! tx :execute!
                    [{:some :results}
                     {:for :you}])
        (let [result (int/query-many repo {:user/id user-id})
              [{:keys [from select where]}] (colls/only! (stubs/calls tx :execute!))]
          (testing "sends a query to the repository"
            (is (= #{[:teams.name "team/name"]
                     [:teams.type "team/type"]
                     [:teams.id "team/id"]
                     [:teams.created-at "team/created-at"]}
                   (set select)))
            (is (= [:teams] from))
            (is (= [:exists {:select #{1}
                             :from   [:user-teams]
                             :where  [:and
                                      #{[:= #{:user-teams.team-id :teams.id}]
                                        [:= #{:user-teams.user-id user-id}]}]}]
                   (-> where
                       (update 1 (fns/=> (update :select set)
                                         (update :where (fns/=> (update 1 tu/op-set)
                                                                (update 2 tu/op-set)
                                                                tu/op-set))))))))

          (testing "returns the results"
            (is (= [{:some :results} {:for :you}] result)))))

      (testing "when querying the repository throws"
        (stubs/use! tx :execute!
                    (ex-info "Bad" {}))
        (testing "fails")))))

(deftest query-by-id-test
  (testing "query-by-id"
    (let [tx (trepos/stub-transactor ->team-executor)
          repo (rteams/->TeamAccessor tx)
          [team-id user-id] (repeatedly uuids/random)]
      (testing "when querying teams"
        (stubs/use! tx :execute!
                    [{:some :result}]
                    [{:with :team}])
        (let [result (int/query-one repo {:user/id user-id
                                          :team/id team-id})
              [[select-team] [select-members]] (colls/only! 2 (stubs/calls tx :execute!))]
          (testing "queries the team from the repository"
            (let [{:keys [from select where]} select-team]
              (is (= #{[:teams.name "team/name"]
                       [:teams.type "team/type"]
                       [:teams.id "team/id"]
                       [:teams.created-at "team/created-at"]}
                     (set select)))
              (is (= [:teams] from))
              (let [[clause & clauses] where
                    clauses' (into {} (map (juxt tu/op-set identity)) clauses)]
                (is (= :and clause))
                (is (contains? clauses' [:= #{:teams.id team-id}]))
                (is (= [:exists {:select #{1}
                                 :from   [:user-teams]
                                 :where  [:and
                                          #{[:= #{:user-teams.team-id :teams.id}]
                                            [:= #{:user-teams.user-id user-id}]}]}]
                       (-> clauses'
                           (dissoc [:= #{:teams.id team-id}])
                           colls/only!
                           val
                           (update 1 (fns/=> (update :select set)
                                             (update :where (fns/=> (update 1 tu/op-set)
                                                                    (update 2 tu/op-set)
                                                                    tu/op-set))))))))))

          (testing "queries the team members from the repository"
            (let [{:keys [from select where]} select-members]
              (is (= #{[:member.id "member/id"]
                       [:member.first-name "member/first-name"]
                       [:member.last-name "member/last-name"]
                       [:user-teams.team-id "member/team-id"]}
                     (set select)))
              (is (= [[:users :member]] from))
              (is (= [:= #{:user-teams.team-id team-id}]
                     (tu/op-set where)))))


          (testing "returns the results"
            (is (= {:some :result :team/members [{:with :team}]} result)))))

      (testing "when querying the repository throws"
        (stubs/use! tx :execute!
                    (ex-info "Bad" {}))
        (testing "fails")))))

(deftest create!-test
  (testing "create!"
    (let [pubsub (stubs/create (reify
                                 ppubsub/IPub
                                 (publish! [_ _ _])
                                 ppubsub/ISub
                                 (subscribe! [_ _ _ _])
                                 (unsubscribe! [_ _])
                                 (unsubscribe! [_ _ _])))
          tx (trepos/stub-transactor (->team-executor {:pubsub pubsub}))
          repo (rteams/->TeamAccessor tx)
          handler (rteams/command-handler {:accessor repo
                                           :pubsub   pubsub})
          [team-id user-id] (repeatedly uuids/random)
          team {:team/id   team-id
                :team/name "some team"}]
      (testing "when creating a team"
        (stubs/use! tx :execute!
                    [{:id team-id}]
                    nil
                    [team]
                    [{:id "event-id"}])
        (handler {:msg [::id
                        {:command/type :team/create!
                         :command/data {:created-at :whenever
                                        :other      :junk}}
                        {:user/id user-id}]})

        (let [[[insert-team] [insert-user-team] [query-for-event]] (colls/only! 3 (stubs/calls tx :execute!))]
          (testing "saves to the repository"
            (is (= {:insert-into :teams
                    :values      [{:created-at :whenever}]
                    :returning   [:id]}
                   insert-team))
            (is (= {:insert-into :user-teams
                    :values      [{:user-id user-id
                                   :team-id team-id}]
                    :returning   [:*]}
                   insert-user-team)))

          (testing "queries from the repository"
            (is (= {:select #{[:teams.created-at "team/created-at"]
                              [:teams.name "team/name"]
                              [:teams.type "team/type"]
                              [:teams.id "team/id"]}
                    :from   [:teams]
                    :where  [:= #{:teams.id team-id}]}
                   (-> query-for-event
                       (select-keys #{:select :from :where})
                       (update :select set)
                       (update :where tu/op-set))))))

        (testing "emits an event"
          (let [[topic [event-id event]] (colls/only! (stubs/calls pubsub :publish!))]
            (is (= [::ps/user user-id] topic))
            (is (uuid? event-id))
            (is (= {:event/id         event-id
                    :event/type       :team/created
                    :event/model-id   team-id
                    :event/data       team
                    :event/emitted-by user-id}
                   event)))))

      (testing "when the executor throws an exception"
        (let [request-id (uuids/random)
              user-id (uuids/random)]
          (stubs/init! pubsub)
          (stubs/use! tx :execute!
                      (ex-info "Executor" {}))
          (handler {:msg [::id
                          {:command/type :team/create!
                           :command/data {}}
                          {:user/id user-id :request/id request-id}]})

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
                      :event/data       {:error/command :team/create!
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
                      [{:id "team-id"}])
          (stubs/use! pubsub :publish!
                      (ex-info "Pubsub" {}))
          (handler {:msg [::id
                          {:command/type :team/create!
                           :command/data {}}
                          {:user/id user-id :request/id request-id}]})

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
                      :event/data       {:error/command :team/create!
                                         :error/reason  "Pubsub"}
                      :event/emitted-by user-id}
                     event))
              (is (= {:request/id request-id
                      :user/id    user-id}
                     ctx)))))))))
