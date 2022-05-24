(ns ^:unit audiophile.backend.infrastructure.pubsub.handlers.projects-test
  (:require
    [clojure.test :refer [are deftest is testing]]
    [audiophile.backend.domain.interactors.core :as int]
    [audiophile.backend.api.pubsub.core :as ps]
    [audiophile.backend.infrastructure.pubsub.handlers.projects :as pub.projects]
    [audiophile.common.core.utils.colls :as colls]
    [audiophile.common.core.utils.logger :as log]
    [audiophile.common.core.utils.uuids :as uuids]
    [audiophile.test.utils :as tu]
    [audiophile.test.utils.repositories :as trepos]
    [audiophile.test.utils.services :as ts]
    [audiophile.test.utils.stubs :as stubs]))

(deftest handle!-test
  (testing "(ProjectCommandHandler#handle!)"
    (let [ch (ts/->chan)
          tx (trepos/stub-transactor trepos/->project-executor)
          handler (pub.projects/->ProjectCommandHandler tx ch)
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
        (int/handle! handler
                     {:command/type :project/create!
                      :command/data {:created-at      :whenever
                                     :project/team-id team-id
                                     :other           :junk}
                      :command/ctx  {:user/id user-id}})

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
          (let [{event-id :event/id :as event} (-> ch
                                                   (stubs/calls :send!)
                                                   colls/only!
                                                   first)]
            (is (uuid? event-id))
            (is (= {:event/id         event-id
                    :event/type       :project/created
                    :event/model-id   project-id
                    :event/data       project
                    :event/emitted-by user-id
                    :event/ctx        {:user/id user-id}}
                   event)))))

      (testing "when the executor throws an exception"
        (let [request-id (uuids/random)
              user-id (uuids/random)]
          (stubs/init! ch)
          (stubs/use! tx :execute!
                      (ex-info "Executor" {}))
          (is (thrown? Throwable
                       (int/handle! handler
                                    {:command/type :project/create!
                                     :command/data {}
                                     :command/ctx  {:user/id    user-id
                                                    :request/id request-id}})))

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
          (is (thrown? Throwable
                       (int/handle! handler
                                    {:command/type :project/create!
                                     :command/data {}
                                     :command/ctx  {:user/id    user-id
                                                    :request/id request-id}})))

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
