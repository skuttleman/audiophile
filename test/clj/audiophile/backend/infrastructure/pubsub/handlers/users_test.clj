(ns ^:unit audiophile.backend.infrastructure.pubsub.handlers.users-test
  (:require
    [audiophile.backend.domain.interactors.core :as int]
    [audiophile.backend.infrastructure.repositories.core :as repos]
    [audiophile.backend.infrastructure.templates.workflows :as wf]
    [audiophile.common.core.utils.colls :as colls]
    [audiophile.common.core.utils.maps :as maps]
    [audiophile.common.core.utils.uuids :as uuids]
    [audiophile.test.utils.repositories :as trepos]
    [audiophile.test.utils.services :as ts]
    [audiophile.test.utils.stubs :as stubs]
    [clojure.test :refer [are deftest is testing]]
    audiophile.backend.infrastructure.pubsub.handlers.users))

(deftest user-create!-test
  (testing "wf/command-handler :user/create!"
    (let [commands (ts/->chan)
          events (ts/->chan)
          tx (trepos/stub-transactor)
          [signup-id user-id spigot-id] (repeatedly uuids/random)
          user {:user/id user-id}]
      (testing "when creating a user"
        (stubs/use! tx :execute!
                    [{:id user-id}])
        (repos/transact! tx wf/command-handler
                         (maps/->m commands events)
                         {:command/type :user/create!
                          :command/data {:spigot/id     spigot-id
                                         :spigot/params {:created-at :whenever
                                                         :other      :junk}}
                          :command/ctx  {:user/id signup-id}})

        (let [[insert-user] (colls/only! (stubs/calls tx :execute!))]
          (testing "saves to the repository"
            (is (= {:insert-into :users
                    :values      [{:created-at :whenever}]
                    :returning   [:id]}
                   insert-user))))

        (testing "emits a command"
          (let [{command-id :command/id :as command} (-> commands
                                                         (stubs/calls :send!)
                                                         colls/only!
                                                         first)]
            (is (uuid? command-id))
            (is (= {:command/ctx        {:user/id signup-id}
                    :command/data       {:spigot/id     spigot-id
                                         :spigot/result {:user/id user-id}}
                    :command/emitted-by signup-id
                    :command/id         command-id
                    :command/type       :workflow/next!}

                   command)))))

      (testing "when the executor throws an exception"
        (let [request-id (uuids/random)
              user-id (uuids/random)]
          (stubs/init! events)
          (stubs/use! tx :execute!
                      (ex-info "Executor" {}))
          (repos/transact! tx wf/command-handler
                           (maps/->m commands events)
                           {:command/type :user/create!
                            :command/data {:spigot/params {}}
                            :command/ctx  {:user/id    user-id
                                           :request/id request-id}})

          (testing "emits a command-failed event"
            (let [{event-id :event/id :as event} (-> events
                                                     (stubs/calls :send!)
                                                     colls/only!
                                                     first)]
              (is (uuid? event-id))
              (is (= {:event/id         event-id
                      :event/model-id   request-id
                      :event/type       :command/failed
                      :event/data       {:error/command :user/create!
                                         :error/reason  "Executor"}
                      :event/emitted-by user-id
                      :event/ctx        {:request/id request-id
                                         :user/id    user-id}}
                     event))))))

      (testing "when the pubsub throws an exception"
        (let [request-id (uuids/random)
              user-id (uuids/random)]
          (stubs/init! events)
          (stubs/use! tx :execute!
                      [{:id user-id}])
          (stubs/use! commands :send!
                      (ex-info "Channel" {}))
          (repos/transact! tx wf/command-handler
                           (maps/->m commands events)
                           {:command/type :user/create!
                            :command/data {}
                            :command/ctx  {:user/id    user-id
                                           :signup/id  signup-id
                                           :request/id request-id}})

          (testing "emits a command-failed event"
            (let [{event-id :event/id :as event} (-> events
                                                     (stubs/calls :send!)
                                                     colls/only!
                                                     first)]
              (is (uuid? event-id))
              (is (= {:event/id         event-id
                      :event/model-id   request-id
                      :event/type       :command/failed
                      :event/data       {:error/command :user/create!
                                         :error/reason  "Channel"}
                      :event/emitted-by user-id
                      :event/ctx        {:request/id request-id
                                         :signup/id  signup-id
                                         :user/id    user-id}}
                     event)))))))))
