(ns ^:unit audiophile.backend.infrastructure.pubsub.handlers.projects-test
  (:require
    [audiophile.backend.infrastructure.repositories.core :as repos]
    [audiophile.backend.infrastructure.templates.workflows :as wf]
    [audiophile.common.core.utils.colls :as colls]
    [audiophile.common.core.utils.logger :as log]
    [audiophile.common.core.utils.uuids :as uuids]
    [audiophile.test.utils :as tu]
    [audiophile.test.utils.repositories :as trepos]
    [audiophile.test.utils.services :as ts]
    [audiophile.test.utils.stubs :as stubs]
    [clojure.test :refer [are deftest is testing]]
    audiophile.backend.infrastructure.pubsub.handlers.projects))

(deftest handle!-test
  (testing "(ProjectCommandHandler#handle!)"
    (let [ch (ts/->chan)
          tx (trepos/stub-transactor)
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
        (repos/transact! tx wf/command-handler
                         {:commands ch}
                         {:command/type :project/create!
                          :command/data {:spigot/id     spigot-id
                                         :spigot/params {:created-at      :whenever
                                                         :project/team-id team-id
                                                         :other           :junk}}
                          :command/ctx  {:user/id user-id}})

        (let [[[access] [insert]] (colls/only! 2 (stubs/calls tx :execute!))]
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
                   insert))))

        (testing "emits an command"
          (let [{command-id :command/id :as command} (-> ch
                                                         (stubs/calls :send!)
                                                         colls/only!
                                                         first)]
            (is (uuid? command-id))
            (is (= {:command/id         command-id
                    :command/type       :workflow/next!
                    :command/data       {:spigot/id     spigot-id
                                         :spigot/result {:project/id project-id}}
                    :command/emitted-by user-id
                    :command/ctx        {:user/id user-id}}
                   command)))))

      (testing "when the executor throws an exception"
        (let [request-id (uuids/random)
              user-id (uuids/random)]
          (stubs/init! ch)
          (stubs/use! tx :execute!
                      (ex-info "Executor" {}))
          (repos/transact! tx wf/command-handler
                           {:events ch}
                           {:command/type :project/create!
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
                      :event/data       {:error/command :project/create!
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
                      [{:id "project-id"}])
          (stubs/use! ch :send!
                      (ex-info "Channel" {}))
          (repos/transact! tx wf/command-handler
                           {:commands ch :events ch}
                           {:command/type :project/create!
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
                      :event/data       {:error/command :project/create!
                                         :error/reason  "Channel"}
                      :event/emitted-by user-id
                      :event/ctx        {:request/id request-id
                                         :user/id    user-id}}
                     event)))))))))
