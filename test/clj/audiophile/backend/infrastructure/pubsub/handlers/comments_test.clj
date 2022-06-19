(ns ^:unit audiophile.backend.infrastructure.pubsub.handlers.comments-test
  (:require
    [audiophile.backend.domain.interactors.core :as int]
    [audiophile.backend.infrastructure.db.comments :as db.comments]
    [audiophile.backend.infrastructure.pubsub.handlers.comments :as pub.comments]
    [audiophile.common.core.utils.colls :as colls]
    [audiophile.common.core.utils.fns :as fns]
    [audiophile.common.core.utils.uuids :as uuids]
    [audiophile.test.utils :as tu]
    [audiophile.test.utils.repositories :as trepos]
    [audiophile.test.utils.services :as ts]
    [audiophile.test.utils.stubs :as stubs]
    [clojure.test :refer [are deftest is testing]]))

(deftest handle!-test
  (testing "(CommentCommandHandler#handle!)"
    (let [ch (ts/->chan)
          tx (trepos/stub-transactor db.comments/->CommentsRepoExecutor)
          handler (pub.comments/->CommentCommandHandler tx ch)
          [comment-id file-version-id user-id] (repeatedly uuids/random)
          comment {:comment/id              comment-id
                   :comment/name            "some comment"
                   :comment/file-version-id file-version-id}]
      (testing "when creating a comment"
        (stubs/use! tx :execute!
                    [{:id "team-id"}]
                    [{:id comment-id}]
                    [comment])
        (int/handle! handler
                     {:command/type :comment/create!
                      :command/data {:created-at              :whenever
                                     :comment/file-version-id file-version-id
                                     :other                   :junk}
                      :command/ctx  {:user/id user-id}})

        (let [[[access] [insert] [query-for-event]] (colls/only! 3 (stubs/calls tx :execute!))]
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
                   insert)))

          (testing "queries from the repository"
            (is (= {:select #{[:comments.id "comment/id"]
                              [:comments.comment-id "comment/comment-id"]
                              [:comments.body "comment/body"]
                              [:comments.selection "comment/selection"]
                              [:comments.created-at "comment/created-at"]
                              [:comments.file-version-id "comment/file-version-id"]}
                    :from   [:comments]
                    :where  [:= #{:comments.id comment-id}]}
                   (-> query-for-event
                       (update :select set)
                       (update :where tu/op-set))))))

        (testing "emits an event"
          (let [{event-id :event/id :as event} (-> ch
                                                   (stubs/calls :send!)
                                                   colls/only!
                                                   first)]
            (is (uuid? event-id))
            (is (= {:event/id         event-id
                    :event/type       :comment/created
                    :event/model-id   comment-id
                    :event/data       comment
                    :event/emitted-by user-id
                    :event/ctx        {:user/id user-id}}
                   event)))))

      (testing "when the executor throws an exception"
        (let [request-id (uuids/random)
              user-id (uuids/random)]
          (stubs/init! ch)
          (stubs/use! tx :execute!
                      (ex-info "Executor" {}))
          (int/handle! handler
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
          (int/handle! handler
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
