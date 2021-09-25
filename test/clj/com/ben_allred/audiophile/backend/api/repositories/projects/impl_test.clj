(ns ^:unit com.ben-allred.audiophile.backend.api.repositories.projects.impl-test
  (:require
    [clojure.test :refer [are deftest is testing]]
    [com.ben-allred.audiophile.backend.api.repositories.projects.impl :as rprojects]
    [com.ben-allred.audiophile.backend.domain.interactors.core :as int]
    [com.ben-allred.audiophile.common.core.utils.colls :as colls]
    [com.ben-allred.audiophile.common.core.utils.fns :as fns]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.common.core.utils.uuids :as uuids]
    [com.ben-allred.audiophile.test.utils :as tu]
    [com.ben-allred.audiophile.test.utils.repositories :as trepos]
    [com.ben-allred.audiophile.test.utils.stubs :as stubs]))

(deftest query-all-test
  (testing "query-all"
    (let [tx (trepos/stub-transactor trepos/->project-executor)
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
    (let [tx (trepos/stub-transactor trepos/->project-executor)
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

#_(deftest create!-test
  (testing "create!"
    ;; TODO - write test
    ))
