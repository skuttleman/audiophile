(ns ^:unit audiophile.backend.api.repositories.comments.impl-test
  (:require
    [clojure.test :refer [are deftest is testing]]
    [audiophile.backend.api.repositories.comments.impl :as rcomments]
    [audiophile.backend.domain.interactors.core :as int]
    [audiophile.common.core.utils.colls :as colls]
    [audiophile.common.core.utils.fns :as fns]
    [audiophile.common.core.utils.logger :as log]
    [audiophile.common.core.utils.uuids :as uuids]
    [audiophile.test.utils :as tu]
    [audiophile.test.utils.assertions :as assert]
    [audiophile.test.utils.repositories :as trepos]
    [audiophile.test.utils.services :as ts]
    [audiophile.test.utils.stubs :as stubs]))

(deftest query-all-test
  (testing "query-all"
    (let [tx (trepos/stub-transactor trepos/->comment-executor)
          repo (rcomments/->CommentAccessor tx nil)
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
    (let [ch (ts/->chan)
          repo (rcomments/->CommentAccessor nil ch)
          [user-id request-id] (repeatedly uuids/random)]
      (testing "emits a command"
        (int/create! repo {:some :data} {:some       :opts
                                         :some/other :opts
                                         :user/id    user-id
                                         :request/id request-id})
        (assert/is? {:command/id         uuid?
                     :command/type       :comment/create!
                     :command/data       {:some :data}
                     :command/emitted-by user-id
                     :command/ctx        {:user/id    user-id
                                          :request/id request-id}}
                    (first (colls/only! (stubs/calls ch :send!))))))))
