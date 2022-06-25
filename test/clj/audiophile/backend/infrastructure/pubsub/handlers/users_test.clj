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
        (let [result (repos/transact! tx wf/command-handler
                                      (maps/->m commands events)
                                      {:command/type :user/create!
                                       :command/data {:spigot/id     spigot-id
                                                      :spigot/params {:created-at :whenever
                                                                      :other      :junk}}
                                       :command/ctx  {:user/id signup-id}})
              [insert-user] (colls/only! (stubs/calls tx :execute!))]
          (testing "saves to the repository"
            (is (= {:insert-into :users
                    :values      [{:created-at :whenever}]
                    :returning   [:id]}
                   insert-user)))

          (testing "returns the result"
            (is (= {:user/id user-id} result)))))

      (testing "when the executor throws an exception"
        (let [request-id (uuids/random)
              user-id (uuids/random)]
          (stubs/init! events)
          (stubs/use! tx :execute!
                      (ex-info "Executor" {}))
          (testing "throws an exception"
            (is (thrown? Throwable (repos/transact! tx wf/command-handler
                                                    (maps/->m commands events)
                                                    {:command/type :user/create!
                                                     :command/data {:spigot/params {}}
                                                     :command/ctx  {:user/id    user-id
                                                                    :request/id request-id}})))))))))
