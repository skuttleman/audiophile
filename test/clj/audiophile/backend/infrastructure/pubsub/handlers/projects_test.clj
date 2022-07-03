(ns ^:unit audiophile.backend.infrastructure.pubsub.handlers.projects-test
  (:require
    [audiophile.backend.infrastructure.repositories.core :as repos]
    [audiophile.backend.infrastructure.templates.workflows :as wf]
    [audiophile.common.core.utils.colls :as colls]
    [audiophile.common.core.utils.logger :as log]
    [audiophile.common.core.utils.uuids :as uuids]
    [audiophile.test.utils :as tu]
    [audiophile.test.utils.services :as ts]
    [audiophile.test.utils.stubs :as stubs]
    [clojure.test :refer [are deftest is testing]]
    audiophile.backend.infrastructure.pubsub.handlers.projects))

(deftest project-create!-test
  (testing "wf/command-handler :project/create!"
    (let [ch (ts/->chan)
          tx (ts/->tx)
          [project-id team-id user-id spigot-id] (repeatedly uuids/random)
          project {:project/id      project-id
                   :project/name    "some project"
                   :project/team-id team-id}]
      (testing "when creating a project"
        (stubs/use! tx :execute!
                    [{:id "team-id"}]
                    [{:id project-id}]
                    [project]
                    [{:id "event-id"}])
        (let [result (repos/transact! tx wf/command-handler
                                      {:commands ch}
                                      {:command/type :project/create!
                                       :command/data {:spigot/id     spigot-id
                                                      :spigot/params {:created-at      :whenever
                                                                      :project/team-id team-id
                                                                      :other           :junk}}
                                       :command/ctx  {:user/id user-id}})
              [[access] [insert]] (colls/only! 2 (stubs/calls tx :execute!))]
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

          (testing "returns the result"
            (is (= {:project/id project-id} result)))))

      (testing "when the executor throws an exception"
        (let [request-id (uuids/random)
              user-id (uuids/random)]
          (stubs/init! ch)
          (stubs/use! tx :execute!
                      (ex-info "Executor" {}))
          (testing "throws an exception"
            (is (thrown? Throwable (repos/transact! tx wf/command-handler
                                                    {:events ch}
                                                    {:command/type :project/create!
                                                     :command/data {}
                                                     :command/ctx  {:user/id    user-id
                                                                    :request/id request-id}})))))))))
