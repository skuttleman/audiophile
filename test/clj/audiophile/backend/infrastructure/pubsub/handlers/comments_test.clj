(ns ^:unit audiophile.backend.infrastructure.pubsub.handlers.comments-test
  (:require
    [audiophile.backend.infrastructure.repositories.core :as repos]
    [audiophile.backend.infrastructure.templates.workflows :as wf]
    [audiophile.common.core.utils.colls :as colls]
    [audiophile.common.core.utils.fns :as fns]
    [audiophile.common.core.utils.uuids :as uuids]
    [audiophile.test.utils :as tu]
    [audiophile.test.utils.repositories :as trepos]
    [audiophile.test.utils.services :as ts]
    [audiophile.test.utils.stubs :as stubs]
    [clojure.test :refer [are deftest is testing]]
    audiophile.backend.infrastructure.pubsub.handlers.comments))

(deftest handle!-test
  (testing "(CommentCommandHandler#handle!)"
    (let [ch (ts/->chan)
          tx (trepos/stub-transactor)
          [comment-id file-version-id user-id spigot-id] (repeatedly uuids/random)
          comment {:comment/id              comment-id
                   :comment/name            "some comment"
                   :comment/file-version-id file-version-id}]
      (testing "when creating a comment"
        (stubs/use! tx :execute!
                    [{:id "team-id"}]
                    [{:id comment-id}]
                    [comment])
        (repos/transact! tx wf/command-handler
                         {:commands ch}
                         {:command/type :comment/create!
                          :command/data {:spigot/id     spigot-id
                                         :spigot/params {:created-at              :whenever
                                                         :comment/file-version-id file-version-id
                                                         :other                   :junk}}
                          :command/ctx  {:user/id user-id}})

        (let [[[access] [insert]] (colls/only! 2 (stubs/calls tx :execute!))]
          (testing "verifies file access"
            (is (= {:select #{1}
                    :from   [:projects]
                    :where  [:and
                             #{[:= #{:user-teams.user-id user-id}]
                               [:= #{:file-versions.id file-version-id}]}]
                    :join   [:user-teams [:= #{:user-teams.team-id :projects.team-id}]
                             :files [:= #{:files.project-id :projects.id}]
                             :file-versions [:= #{:file-versions.file-id :files.id}]]}
                   (-> access
                       (update :select set)
                       (update :where (fns/=> (update 1 tu/op-set)
                                              (update 2 tu/op-set)
                                              tu/op-set))
                       (update :join (fns/=> (update 1 tu/op-set)
                                             (update 3 tu/op-set)
                                             (update 5 tu/op-set)))))))

          (testing "saves to the repository"
            (is (= {:insert-into :comments
                    :values      [{:created-at      :whenever
                                   :file-version-id file-version-id}]
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
                                         :spigot/result {:comment/id comment-id}}
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
                           {:command/type :comment/create!
                            :command/ctx  {:user/id user-id :request/id request-id}})

          (testing "emits a command-failed event"
            (let [{event-id :event/id :as event} (-> ch
                                                     (stubs/calls :send!)
                                                     colls/only!
                                                     first)]
              (is (uuid? event-id))
              (is (= {:event/id         event-id
                      :event/model-id   request-id
                      :event/type       :command/failed
                      :event/data       {:error/command :comment/create!
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
                      [{:id "comment-id"}]
                      [{:id comment-id}]
                      [comment])
          (stubs/use! ch :send!
                      (ex-info "Channel" {}))
          (repos/transact! tx wf/command-handler
                           {:commands ch :events ch}
                           {:command/type :comment/create!
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
                      :event/data       {:error/command :comment/create!
                                         :error/reason  "Channel"}
                      :event/emitted-by user-id
                      :event/ctx        {:request/id request-id
                                         :user/id    user-id}}
                     event)))))))))
