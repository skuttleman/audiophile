(ns ^:unit com.ben-allred.audiophile.backend.api.repositories.comments.impl-test
  (:require
    [clojure.test :refer [are deftest is testing]]
    [com.ben-allred.audiophile.backend.api.repositories.comments.impl :as rcomments]
    [com.ben-allred.audiophile.backend.domain.interactors.core :as int]
    [com.ben-allred.audiophile.backend.domain.interactors.protocols :as pint]
    [com.ben-allred.audiophile.backend.infrastructure.db.comments :as db.comments]
    [com.ben-allred.audiophile.backend.infrastructure.db.events :as db.events]
    [com.ben-allred.audiophile.backend.infrastructure.pubsub.ws :as ws]
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
  ([executor {:keys [comments emitter event-types events file-versions
                     files projects pubsub user-events user-teams users]
              :as   models}]
   (let [comment-exec (db.comments/->CommentsRepoExecutor executor
                                                          comments
                                                          projects
                                                          files
                                                          file-versions
                                                          user-teams
                                                          users)
         event-exec (db.events/->EventsExecutor executor
                                                event-types
                                                events
                                                user-events
                                                (db.events/->conform-fn models))
         emitter* (db.comments/->CommentsEventEmitter event-exec emitter pubsub)]
     (db.comments/->Executor comment-exec emitter*))))

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
              [{:keys [from select where] :as foo}] (colls/only! (stubs/calls tx :execute!))]
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
                                 ppubsub/IPubSub
                                 (publish! [_ _ _])
                                 (subscribe! [_ _ _ _])
                                 (unsubscribe! [_ _])
                                 (unsubscribe! [_ _ _])))
          emitter (stubs/create (reify
                                  pint/IEmitter
                                  (command-failed! [_ _ _])))
          tx (trepos/stub-transactor (->comment-executor {:emitter emitter
                                                          :pubsub  pubsub}))
          repo (rcomments/->CommentAccessor tx)
          [comment-id file-version-id user-id] (repeatedly uuids/random)
          comment {:comment/id              comment-id
                   :comment/name            "some comment"
                   :comment/file-version-id file-version-id}]
      (testing "when creating a comment"
        (stubs/use! tx :execute!
                    [{:id "team-id"}]
                    [{:id comment-id}]
                    [comment]
                    [{:id "event-id"}])
        @(int/create! repo
                      {:created-at              :whenever
                       :comment/file-version-id file-version-id
                       :other                   :junk}
                      {:user/id user-id})
        (let [[[access] [insert] [query-for-event] [insert-event]] (colls/only! 4 (stubs/calls tx :execute!))]
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
                       (update :where tu/op-set)))))

          (testing "inserts the event in the repository"
            (let [{:keys [event-type-id] :as value} (colls/only! (:values insert-event))]
              (is (= {:insert-into :events
                      :returning   [:id]}
                     (dissoc insert-event :values)))
              (is (= {:model-id   comment-id
                      :data       {:comment/id              comment-id
                                   :comment/name            "some comment"
                                   :comment/file-version-id file-version-id}
                      :emitted-by user-id}
                     (dissoc value :event-type-id)))
              (is (= {:select #{[:event-types.id "event-type/id"]}
                      :from   [:event-types]
                      :where  [:and #{[:= #{:event-types.category "comment"}]
                                      [:= #{:event-types.name "created"}]}]}
                     (-> event-type-id
                         (update :select set)
                         (update-in [:where 1] tu/op-set)
                         (update-in [:where 2] tu/op-set)
                         (update :where tu/op-set)))))))

        (testing "emits an event"
          (let [[topic [event-id event]] (colls/only! (stubs/calls pubsub :publish!))]
            (is (= [::ws/user user-id] topic))
            (is (= "event-id" event-id))
            (is (= {:event/id         "event-id"
                    :event/type       :comment/created
                    :event/model-id   comment-id
                    :event/data       comment
                    :event/emitted-by user-id}
                   event)))))

      (testing "when the executor throws an exception"
        (let [request-id (uuids/random)
              user-id (uuids/random)]
          (stubs/init! emitter)
          (stubs/use! tx :execute!
                      (ex-info "Executor" {}))
          @(int/create! repo {} {:user/id user-id :request/id request-id})
          (testing "does not emit a successful event"
            (empty? (stubs/calls pubsub :publish!)))

          (testing "emits a command-failed event"
            (is (= [request-id {:user/id       user-id
                                :request/id    request-id
                                :error/command :comment/create}]
                   (-> emitter
                       (stubs/calls :command-failed!)
                       colls/only!
                       (update 1 dissoc :error/reason)))))))

      (testing "when the pubsub throws an exception"
        (let [request-id (uuids/random)
              user-id (uuids/random)]
          (stubs/init! emitter)
          (stubs/use! pubsub :publish!
                      (ex-info "Executor" {}))
          @(int/create! repo {} {:user/id user-id :request/id request-id})
          (testing "emits a command-failed event"
            (is (= [request-id {:user/id       user-id
                                :request/id    request-id
                                :error/command :comment/create}]
                   (-> emitter
                       (stubs/calls :command-failed!)
                       colls/only!
                       (update 1 dissoc :error/reason))))))))))
