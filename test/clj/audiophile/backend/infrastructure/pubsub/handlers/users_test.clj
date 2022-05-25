(ns ^:unit audiophile.backend.infrastructure.pubsub.handlers.users-test
  (:require
    [clojure.test :refer [are deftest is testing]]
    [audiophile.backend.domain.interactors.core :as int]
    [audiophile.backend.api.pubsub.core :as ps]
    [audiophile.backend.infrastructure.pubsub.handlers.users :as pub.users]
    [audiophile.common.core.utils.colls :as colls]
    [audiophile.common.core.utils.uuids :as uuids]
    [audiophile.test.utils :as tu]
    [audiophile.test.utils.repositories :as trepos]
    [audiophile.test.utils.services :as ts]
    [audiophile.test.utils.stubs :as stubs]))

(deftest handle!-test
  (testing "(UserCommandHandler#handle!)"
    (let [commands (ts/->chan)
          events (ts/->chan)
          tx (trepos/stub-transactor trepos/->user-executor)
          handler (pub.users/->UserCommandHandler tx commands events)
          [signup-id user-id] (repeatedly uuids/random)
          user {:user/id user-id}]
      (testing "when creating a user"
        (stubs/use! tx :execute!
                    [{:id user-id}])
        (int/handle! handler
                     {:command/type :user/create!
                      :command/data {:created-at :whenever
                                     :other      :junk}
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
            (is (= {:command/id         command-id
                    :command/type       :team/create!
                    :command/data       {:team/name "My Personal Projects"
                                         :team/type :PERSONAL}
                    :command/emitted-by user-id
                    :command/ctx        {:signup/id signup-id
                                         :user/id   user-id}}

                   command))))

        (testing "emits an event"
          (let [{event-id :event/id :as event} (-> events
                                                   (stubs/calls :send!)
                                                   colls/only!
                                                   first)]
            (is (uuid? event-id))
            (is (= {:event/id         event-id
                    :event/type       :user/created
                    :event/model-id   user-id
                    :event/data       user
                    :event/emitted-by user-id
                    :event/ctx        {:signup/id signup-id
                                       :user/id   user-id}}
                   event)))))

      (testing "when the executor throws an exception"
        (let [request-id (uuids/random)
              user-id (uuids/random)]
          (stubs/init! events)
          (stubs/use! tx :execute!
                      (ex-info "Executor" {}))
          (int/handle! handler
                       {:command/type :user/create!
                        :command/data {}
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
          (stubs/use! events :send!
                      (ex-info "Channel" {}))
          (int/handle! handler
                       {:command/type :user/create!
                        :command/data {}
                        :command/ctx  {:user/id    signup-id
                                       :request/id request-id}})

          (testing "emits a command-failed event"
            (let [{event-id :event/id :as event} (-> events
                                                     (stubs/calls :send!)
                                                     rest
                                                     colls/only!
                                                     first)]
              (is (uuid? event-id))
              (is (= {:event/id         event-id
                      :event/model-id   request-id
                      :event/type       :command/failed
                      :event/data       {:error/command :user/create!
                                         :error/reason  "Channel"}
                      :event/emitted-by nil
                      :event/ctx        {:request/id request-id
                                         :signup/id  signup-id}}
                     event)))))))))
