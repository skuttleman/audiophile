(ns ^:unit com.ben-allred.audiophile.backend.infrastructure.pubsub.handlers.projects-test
  (:require
    [clojure.test :refer [are deftest is testing]]
    [com.ben-allred.audiophile.backend.domain.interactors.core :as int]
    [com.ben-allred.audiophile.backend.infrastructure.pubsub.core :as ps]
    [com.ben-allred.audiophile.backend.infrastructure.pubsub.handlers.projects :as pub.projects]
    [com.ben-allred.audiophile.common.core.utils.colls :as colls]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.common.core.utils.uuids :as uuids]
    [com.ben-allred.audiophile.test.utils :as tu]
    [com.ben-allred.audiophile.test.utils.repositories :as trepos]
    [com.ben-allred.audiophile.test.utils.services :as ts]
    [com.ben-allred.audiophile.test.utils.stubs :as stubs]))

(deftest handle!-test
  (testing "(ProjectCommandHandler#handle!)"
    (let [pubsub (ts/->pubsub)
          tx (trepos/stub-transactor (trepos/->project-executor {:pubsub pubsub}))
          handler (pub.projects/->ProjectCommandHandler tx pubsub)
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
                     {:msg [::id
                            {:command/type :project/create!
                             :command/data {:created-at      :whenever
                                            :project/team-id team-id
                                            :other           :junk}}
                            {:user/id user-id}]})

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
          (let [[topic [event-id event]] (colls/only! (stubs/calls pubsub :publish!))]
            (is (= [::ps/user user-id] topic))
            (is (uuid? event-id))
            (is (= {:event/id         event-id
                    :event/type       :project/created
                    :event/model-id   project-id
                    :event/data       project
                    :event/emitted-by user-id}
                   event)))))

      (testing "when the executor throws an exception"
        (let [request-id (uuids/random)
              user-id (uuids/random)]
          (stubs/init! pubsub)
          (stubs/use! tx :execute!
                      (ex-info "Executor" {}))
          (int/handle! handler
                       {:msg [::id
                              {:command/type :project/create!
                               :command/data {}}
                              {:user/id    user-id
                               :request/id request-id}]})

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
                      :event/data       {:error/command :project/create!
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
                      [{:id "project-id"}])
          (stubs/use! pubsub :publish!
                      (ex-info "Pubsub" {}))
          (int/handle! handler
                       {:msg [::id
                              {:command/type :project/create!
                               :command/data {}}
                              {:user/id    user-id
                               :request/id request-id}]})

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
                      :event/data       {:error/command :project/create!
                                         :error/reason  "Pubsub"}
                      :event/emitted-by user-id}
                     event))
              (is (= {:request/id request-id
                      :user/id    user-id}
                     ctx)))))))))
