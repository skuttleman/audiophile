(ns ^:unit audiophile.backend.infrastructure.repositories.projects.impl-test
  (:require
    [audiophile.backend.domain.interactors.core :as int]
    [audiophile.backend.infrastructure.repositories.projects.impl :as rprojects]
    [audiophile.backend.infrastructure.templates.workflows :as wf]
    [audiophile.common.core.utils.colls :as colls]
    [audiophile.common.core.utils.fns :as fns]
    [audiophile.common.core.utils.logger :as log]
    [audiophile.common.core.utils.uuids :as uuids]
    [audiophile.test.utils :as tu]
    [audiophile.test.utils.assertions :as assert]
    [audiophile.test.utils.repositories :as trepos]
    [audiophile.test.utils.services :as ts]
    [audiophile.test.utils.stubs :as stubs]
    [clojure.test :refer [are deftest is testing]]))

(deftest query-all-test
  (testing "query-all"
    (let [tx (trepos/stub-transactor)
          repo (rprojects/->ProjectAccessor tx nil)
          user-id (uuids/random)]
      (testing "when querying for projects"
        (stubs/use! tx :execute!
                    [{:some :result}])
        (let [result (int/query-many repo {:user/id user-id})
              [{:keys [select from where]}] (colls/only! (stubs/calls tx :execute!))]
          (testing "sends a query to the repository"
            (is (= #{[:projects.team-id "project/team-id"]
                     [:projects.name "project/name"]
                     [:projects.id "project/id"]
                     [:projects.created-at "project/created-at"]}
                   (set select)))
            (is (= [:projects] from))
            (is (= [:exists {:select #{1}
                             :from   [:user-teams]
                             :where  [:and
                                      #{[:= #{:projects.team-id :user-teams.team-id}]
                                        [:= #{:user-teams.user-id user-id}]}]}]
                   (-> where
                       (update-in [1 :select] set)
                       (update-in [1 :where 1] tu/op-set)
                       (update-in [1 :where 2] tu/op-set)
                       (update-in [1 :where] tu/op-set)))))

          (testing "returns the results"
            (is (= [{:some :result}] result))))))))

(deftest query-by-id-test
  (testing "query-by-id"
    (let [tx (trepos/stub-transactor)
          repo (rprojects/->ProjectAccessor tx nil)
          [project-id user-id] (repeatedly uuids/random)]
      (testing "when querying for a single project"
        (stubs/use! tx :execute!
                    [{:some :result}])

        (let [result (int/query-one repo {:user/id    user-id
                                          :project/id project-id})]
          (testing "sends a query to the repository"
            (let [[{:keys [from select where]}] (colls/only! (stubs/calls tx :execute!))]
              (is (= [:projects] from))
              (is (= #{[:projects.team-id "project/team-id"]
                       [:projects.name "project/name"]
                       [:projects.id "project/id"]
                       [:projects.created-at "project/created-at"]}
                     (set select)))
              (let [[clause clauses] (tu/op-set where)
                    clauses' (into {} (map (juxt tu/op-set identity)) clauses)]
                (is (= :and clause))
                (is (contains? clauses'
                               [:= #{:projects.id project-id}]))
                (is (= [:exists {:select #{1}
                                 :from   [:user-teams]
                                 :where  [:and
                                          #{[:= #{:projects.team-id :user-teams.team-id}]
                                            [:= #{:user-teams.user-id user-id}]}]}]
                       (-> clauses'
                           (dissoc [:= #{:projects.id project-id}])
                           colls/only!
                           val
                           (update 1 (fns/=> (update :select set)
                                             (update :where (fns/=> (update 1 tu/op-set)
                                                                    (update 2 tu/op-set)
                                                                    tu/op-set))))))))))

          (testing "returns the results"
            (is (= {:some :result} result))))))))

(deftest create!-test
  (testing "create!"
    (let [ch (ts/->chan)
          repo (rprojects/->ProjectAccessor nil ch)
          [user-id request-id] (repeatedly uuids/random)]
      (testing "emits a command"
        (int/create! repo {:some :data} {:some       :opts
                                         :some/other :opts
                                         :user/id    user-id
                                         :request/id request-id})
        (let [{:command/keys [data] :as command} (-> (stubs/calls ch :send!)
                                                     colls/only!
                                                     first)]
          (assert/is? {:workflows/ctx      {}
                       :workflows/template (peek (wf/load! :projects/create))
                       :workflows/->result '{:project/id (sp.ctx/get ?project-id)}}
                      data)
          (assert/is? {:command/id   uuid?
                       :command/type :workflow/create!
                       :command/ctx  {:user/id    user-id
                                      :request/id request-id}}
                      command))))))
