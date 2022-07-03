(ns ^:unit audiophile.backend.infrastructure.pubsub.handlers.teams-test
  (:require
    [audiophile.backend.infrastructure.repositories.core :as repos]
    [audiophile.backend.infrastructure.templates.workflows :as wf]
    [audiophile.common.core.utils.colls :as colls]
    [audiophile.common.core.utils.uuids :as uuids]
    [audiophile.test.utils.services :as ts]
    [audiophile.test.utils.stubs :as stubs]
    [clojure.test :refer [are deftest is testing]]
    audiophile.backend.infrastructure.pubsub.handlers.teams))

(deftest team-create!-test
  (testing "wf/command-handler :team/create!"
    (let [ch (ts/->chan)
          tx (ts/->tx)
          [team-id user-id spigot-id] (repeatedly uuids/random)
          team {:team/id   team-id
                :team/name "some team"}]
      (testing "when creating a team"
        (stubs/use! tx :execute!
                    [{:id team-id}]
                    nil
                    [team]
                    [{:id "event-id"}])
        (let [result (repos/transact! tx wf/command-handler
                                      {:commands ch}
                                      {:command/type :team/create!
                                       :command/data {:spigot/id     spigot-id
                                                      :spigot/params {:created-at :whenever
                                                                      :other      :junk
                                                                      :user/id    user-id}}
                                       :command/ctx  {:user/id user-id}})
              [[insert-team] [insert-user-team]] (colls/only! 2 (stubs/calls tx :execute!))]
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

          (testing "returns the result"
            (is (= {:team/id team-id} result)))))

      (testing "when the executor throws an exception"
        (let [request-id (uuids/random)
              user-id (uuids/random)]
          (stubs/init! ch)
          (stubs/use! tx :execute!
                      (ex-info "Executor" {}))
          (testing "throws an exception"
            (is (thrown? Throwable (repos/transact! tx wf/command-handler
                                                    {:events ch}
                                                    {:command/type :team/create!
                                                     :command/data {}
                                                     :command/ctx  {:user/id    user-id
                                                                    :request/id request-id}})))))))))
