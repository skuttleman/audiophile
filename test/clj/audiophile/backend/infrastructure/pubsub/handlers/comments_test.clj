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

(deftest comment-create!-test
  (testing "wf/command-handler :comment/create!"
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
        (let [result (repos/transact! tx wf/command-handler
                                      {:commands ch}
                                      {:command/type :comment/create!
                                       :command/data {:spigot/id     spigot-id
                                                      :spigot/params {:created-at              :whenever
                                                                      :comment/file-version-id file-version-id
                                                                      :other                   :junk}}
                                       :command/ctx  {:user/id user-id}})
              [[access] [insert]] (colls/only! 2 (stubs/calls tx :execute!))]
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

          (testing "returns the result"
            (is (= {:comment/id comment-id} result)))))

      (testing "when the executor throws an exception"
        (let [request-id (uuids/random)
              user-id (uuids/random)]
          (stubs/init! ch)
          (stubs/use! tx :execute!
                      (ex-info "Executor" {}))
          (testing "throws an exception"
            (is (thrown? Throwable (repos/transact! tx wf/command-handler
                                                    {:events ch}
                                                    {:command/type :comment/create!
                                                     :command/ctx  {:user/id user-id :request/id request-id}})))))))))
