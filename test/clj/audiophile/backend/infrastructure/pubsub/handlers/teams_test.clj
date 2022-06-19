(ns ^:unit audiophile.backend.infrastructure.pubsub.handlers.teams-test
  (:require
    [audiophile.backend.domain.interactors.core :as int]
    [audiophile.backend.infrastructure.db.teams :as db.teams]
    [audiophile.backend.infrastructure.pubsub.handlers.teams :as pub.teams]
    [audiophile.common.core.utils.colls :as colls]
    [audiophile.common.core.utils.uuids :as uuids]
    [audiophile.test.utils :as tu]
    [audiophile.test.utils.repositories :as trepos]
    [audiophile.test.utils.services :as ts]
    [audiophile.test.utils.stubs :as stubs]
    [clojure.test :refer [are deftest is testing]]))

(deftest handle!-test
  (testing "(TeamCommandHandler#handle!)"
    (let [ch (ts/->chan)
          tx (trepos/stub-transactor db.teams/->TeamsRepoExecutor)
          handler (pub.teams/->TeamCommandHandler tx ch)
          [team-id user-id] (repeatedly uuids/random)
          team {:team/id   team-id
                :team/name "some team"}]
      (testing "when creating a team"
        (stubs/use! tx :execute!
                    [{:id team-id}]
                    nil
                    [team]
                    [{:id "event-id"}])
        (int/handle! handler
                     {:command/type :team/create!
                      :command/data {:created-at :whenever
                                     :other      :junk}
                      :command/ctx  {:user/id user-id}})

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
          (let [{event-id :event/id :as event} (-> ch
                                                   (stubs/calls :send!)
                                                   colls/only!
                                                   first)]
            (is (uuid? event-id))
            (is (= {:event/id         event-id
                    :event/type       :team/created
                    :event/model-id   team-id
                    :event/data       team
                    :event/emitted-by user-id
                    :event/ctx        {:user/id user-id}}
                   event)))))

      (testing "when the executor throws an exception"
        (let [request-id (uuids/random)
              user-id (uuids/random)]
          (stubs/init! ch)
          (stubs/use! tx :execute!
                      (ex-info "Executor" {}))
          (int/handle! handler
                       {:command/type :team/create!
                        :command/data {}
                        :command/ctx  {:user/id    user-id
                                       :request/id request-id}})

          (testing "does not emit a successful event"
            (empty? (stubs/calls ch :send!)))

          (testing "emits a command-failed event"
            (let [{event-id :event/id :as event} (-> ch
                                                     (stubs/calls :send!)
                                                     colls/only!
                                                     first)]
              (is (uuid? event-id))
              (is (= {:event/id         event-id
                      :event/model-id   request-id
                      :event/type       :command/failed
                      :event/data       {:error/command :team/create!
                                         :error/reason  "Executor"}
                      :event/emitted-by user-id
                      :event/ctx        {:request/id request-id
                                         :user/id    user-id}}
                     event))))))

      (testing "when the pubsub throws an exception"
        (let [request-id (uuids/random)
              user-id (uuids/random)]
          (stubs/init! ch)
          (stubs/use! tx :execute!
                      [{:id "team-id"}])
          (stubs/use! ch :send!
                      (ex-info "Channel" {}))
          (int/handle! handler
                       {:command/type :team/create!
                        :command/data {}
                        :command/ctx  {:user/id    user-id
                                       :request/id request-id}})

          (testing "emits a command-failed event"
            (let [{event-id :event/id :as event} (-> ch
                                                     (stubs/calls :send!)
                                                     rest
                                                     colls/only!
                                                     first)]
              (is (uuid? event-id))
              (is (= {:event/id         event-id
                      :event/model-id   request-id
                      :event/type       :command/failed
                      :event/data       {:error/command :team/create!
                                         :error/reason  "Channel"}
                      :event/emitted-by user-id
                      :event/ctx        {:request/id request-id
                                         :user/id    user-id}}
                     event)))))))))
