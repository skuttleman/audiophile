(ns ^:unit com.ben-allred.audiophile.backend.api.repositories.comments.impl-test
  (:require
    [clojure.test :refer [are deftest is testing]]
    [com.ben-allred.audiophile.backend.api.repositories.comments.impl :as rcomments]
    [com.ben-allred.audiophile.backend.domain.interactors.core :as int]
    [com.ben-allred.audiophile.backend.infrastructure.db.comments :as db.comments]
    [com.ben-allred.audiophile.backend.infrastructure.pubsub.core :as ps]
    [com.ben-allred.audiophile.common.core.utils.colls :as colls]
    [com.ben-allred.audiophile.common.core.utils.fns :as fns]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.common.core.utils.uuids :as uuids]
    [com.ben-allred.audiophile.common.infrastructure.pubsub.protocols :as ppubsub]
    [test.utils :as tu]
    [test.utils.repositories :as trepos]
    [test.utils.stubs :as stubs]))

(defn ^:private ->comment-executor
  ([config]
   (fn [executor models]
     (->comment-executor executor (merge models config))))
  ([executor {:keys [comments file-versions files projects pubsub user-teams users]}]
   (let [comment-exec (db.comments/->CommentsRepoExecutor executor
                                                          comments
                                                          projects
                                                          files
                                                          file-versions
                                                          user-teams
                                                          users)]
     (db.comments/->Executor comment-exec pubsub))))

(deftest query-all-test
  (testing "query-all"
    (let [tx (trepos/stub-transactor ->comment-executor)
          repo (rcomments/->CommentAccessor tx)
          [file-id user-id] (repeatedly uuids/random)]
      (testing "when querying for projects"
        (stubs/use! tx :execute!
                    [{:some :result}])
        (let [result (int/query-many repo {:user/id user-id
                                           :file/id file-id})
              [{:keys [from select where]}] (colls/only! (stubs/calls tx :execute!))]
          (testing "sends a query to the repository"
            (is (= [:comments] from))
            (is (= #{[:comments.id "comment/id"]
                     [:comments.comment-id "comment/comment-id"]
                     [:comments.body "comment/body"]
                     [:comments.selection "comment/selection"]
                     [:comments.created-at "comment/created-at"]
                     [:comments.file-version-id "comment/file-version-id"]}
                   (set select)))

            (is (= [:exists {:select #{1}
                             :from   [:file-versions]
                             :where  [:and
                                      #{[:= #{:file-versions.id :comments.file-version-id}]
                                        [:= #{:files.id file-id}]
                                        [:= #{:user-teams.user-id user-id}]}]
                             :join   [:files [:= #{:files.id :file-versions.file-id}]
                                      :projects [:= #{:projects.id :files.project-id}]
                                      :user-teams [:= #{:user-teams.team-id :projects.team-id}]]}]
                   (-> where
                       (update 1 (fns/=> (update :select set)
                                         (update :where (fns/=> (update 1 tu/op-set)
                                                                (update 2 tu/op-set)
                                                                (update 3 tu/op-set)
                                                                tu/op-set))
                                         (update :join (fns/=> (update 1 tu/op-set)
                                                               (update 3 tu/op-set)
                                                               (update 5 tu/op-set)))))))))

          (testing "returns the results"
            (is (= [{:some :result}] result))))))))

(deftest create!-test
  (testing "create!"
    (let [pubsub (stubs/create (reify
                                 ppubsub/IPub
                                 (publish! [_ _ _])
                                 ppubsub/ISub
                                 (subscribe! [_ _ _ _])
                                 (unsubscribe! [_ _])
                                 (unsubscribe! [_ _ _])))
          tx (trepos/stub-transactor (->comment-executor {:pubsub pubsub}))
          repo (rcomments/->CommentAccessor tx)
          handler (rcomments/command-handler {:accessor repo
                                              :pubsub   pubsub})
          [comment-id file-version-id user-id] (repeatedly uuids/random)
          comment {:comment/id              comment-id
                   :comment/name            "some comment"
                   :comment/file-version-id file-version-id}]
      (testing "when creating a comment"
        (stubs/use! tx :execute!
                    [{:id "team-id"}]
                    [{:id comment-id}]
                    [comment])
        (handler
          {:msg [::topic
                 {:command/type :comment/create!
                  :command/data {:created-at              :whenever
                                 :comment/file-version-id file-version-id
                                 :other                   :junk}}
                 {:user/id user-id}]})

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
          (let [[topic [event-id event]] (colls/only! (stubs/calls pubsub :publish!))]
            (is (= [::ps/user user-id] topic))
            (is (uuid? event-id))
            (is (= {:event/id         event-id
                    :event/type       :comment/created
                    :event/model-id   comment-id
                    :event/data       comment
                    :event/emitted-by user-id}
                   event)))))

      (testing "when the executor throws an exception"
        (let [request-id (uuids/random)
              user-id (uuids/random)]
          (stubs/init! pubsub)
          (stubs/use! tx :execute!
                      (ex-info "Executor" {}))
          (handler {:msg [::topic
                          {:command/type :comment/create!}
                          {:user/id user-id :request/id request-id}]})

          (testing "emits a command-failed event"
            (let [[topic [event-id event ctx]] (-> pubsub
                                                   (stubs/calls :publish!)
                                                   colls/only!)]
              (is (= [::ps/user user-id] topic))
              (is (uuid? event-id))
              (is (= {:event/id         event-id
                      :event/model-id   request-id
                      :event/type       :command/failed
                      :event/data       {:error/command :comment/create!
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
                      [{:id "comment-id"}]
                      [{:id comment-id}]
                      [comment])
          (stubs/use! pubsub :publish!
                      (ex-info "Pubsub" {}))
          (handler {:msg [::topic
                          {:command/type :comment/create!}
                          {:user/id user-id :request/id request-id}]})

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
                      :event/data       {:error/command :comment/create!
                                         :error/reason  "Pubsub"}
                      :event/emitted-by user-id}
                     event))
              (is (= {:request/id request-id
                      :user/id    user-id}
                     ctx)))))))))
