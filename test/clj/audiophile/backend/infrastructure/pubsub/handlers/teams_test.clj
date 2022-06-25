(ns ^:unit audiophile.backend.infrastructure.pubsub.handlers.teams-test
  (:require
    [audiophile.backend.infrastructure.repositories.core :as repos]
    [audiophile.backend.infrastructure.templates.workflows :as wf]
    [audiophile.common.core.utils.colls :as colls]
    [audiophile.common.core.utils.uuids :as uuids]
    [audiophile.test.utils :as tu]
    [audiophile.test.utils.repositories :as trepos]
    [audiophile.test.utils.services :as ts]
    [audiophile.test.utils.stubs :as stubs]
    [clojure.test :refer [are deftest is testing]]
    audiophile.backend.infrastructure.pubsub.handlers.teams))

(deftest team-create!-test
  (testing "wf/command-handler :team/create!"
    (let [ch (ts/->chan)
          tx (trepos/stub-transactor)
          [team-id user-id spigot-id] (repeatedly uuids/random)
          team {:team/id   team-id
                :team/name "some team"}]
      (testing "when creating a team"
        (stubs/use! tx :execute!
                    [{:id team-id}]
                    nil
                    [team]
                    [{:id "event-id"}])
        (repos/transact! tx wf/command-handler
                         {:commands ch}
                         {:command/type :team/create!
                          :command/data {:spigot/id     spigot-id
                                         :spigot/params {:created-at :whenever
                                                         :other      :junk
                                                         :user/id    user-id}}
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

        (testing "emits a command"
          (let [{command-id :command/id :as command} (-> ch
                                                         (stubs/calls :send!)
                                                         colls/only!
                                                         first)]
            (is (uuid? command-id))
            (is (= {:command/ctx        {:user/id user-id}
                    :command/data       {:spigot/id     spigot-id
                                         :spigot/result {:team/id   team-id
                                                         :team/name "some team"}}
                    :command/emitted-by user-id
                    :command/id         command-id
                    :command/type       :workflow/next!}
                   command)))))

      (testing "when the executor throws an exception"
        (let [request-id (uuids/random)
              user-id (uuids/random)]
          (stubs/init! ch)
          (stubs/use! tx :execute!
                      (ex-info "Executor" {}))
          (repos/transact! tx wf/command-handler
                           {:events ch}
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
                      (ex-info "Channel" {})
                      nil)
          (repos/transact! tx wf/command-handler
                           {:events ch :commands ch}
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
