(ns ^:unit com.ben-allred.audiophile.api.services.interactors.projects.core-test
  (:require
    [clojure.test :refer [are deftest is testing]]
    [com.ben-allred.audiophile.api.services.interactors.core :as int]
    [com.ben-allred.audiophile.api.services.repositories.projects.core :as rprojects]
    [com.ben-allred.audiophile.common.utils.colls :as colls]
    [com.ben-allred.audiophile.common.utils.fns :as fns]
    [com.ben-allred.audiophile.common.utils.logger :as log]
    [com.ben-allred.audiophile.common.utils.uuids :as uuids]
    [test.utils :as tu]
    [test.utils.repositories :as trepos]
    [test.utils.stubs :as stubs]))

(deftest query-all-test
  (testing "query-all"
    (let [tx (trepos/stub-transactor)
          repo (rprojects/->ProjectAccessor tx)
          user-id (uuids/random)]
      (testing "when querying for projects"
        (stubs/use! tx :execute!
                    [{:some :result}])
        (let [result (int/query-many repo {:user/id user-id})
              [{:keys [select from where]}] (colls/only! (stubs/calls tx :execute!))]
          (testing "sends a query to the repository"
            (is (= #{[:projects.team-id "project/team-id"]
                     [:projects.name "project/name"]
                     [:projects.created-by "project/created-by"]
                     [:projects.id "project/id"]
                     [:projects.created-at "project/created-at"]}
                   (set select)))
            (is (= [:projects] from))
            (is (= [:exists {:select #{:user-id}
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
          repo (rprojects/->ProjectAccessor tx)
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
                       [:projects.created-by "project/created-by"]
                       [:projects.id "project/id"]
                       [:projects.created-at "project/created-at"]}
                     (set select)))
              (let [[clause clauses] (tu/op-set where)
                    clauses' (into {} (map (juxt tu/op-set identity)) clauses)]
                (is (= :and clause))
                (is (contains? clauses'
                               [:= #{:projects.id project-id}]))
                (is (= [:exists {:select #{:user-id}
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
    (let [tx (trepos/stub-transactor)
          repo (rprojects/->ProjectAccessor tx)
          [project-id user-id] (repeatedly uuids/random)]
      (testing "when creating a project"
        (stubs/use! tx :execute!
                    [{:id project-id}]
                    [{:transmogrified :value}])
        (let [result (int/create! repo
                                  {:created-at :whenever
                                   :other      :junk
                                   :user/id user-id})
              [[insert] [select] & more] (stubs/calls tx :execute!)]
          (is (empty? more))
          (testing "saves to the repository"
            (is (= {:insert-into :projects
                    :values      [{:created-at :whenever
                                   :created-by user-id}]
                    :returning   [:id]}
                   insert)))

          (testing "queries from the repository"
            (is (= {:select #{[:projects.team-id "project/team-id"]
                              [:projects.name "project/name"]
                              [:projects.created-by "project/created-by"]
                              [:projects.id "project/id"]
                              [:projects.created-at "project/created-at"]}
                    :from   [:projects]
                    :where  [:= #{:projects.id project-id}]}
                   (-> select
                       (select-keys #{:select :from :where})
                       (update :select set)
                       (update :where tu/op-set)))))

          (testing "returns the results"
            (is (= {:transmogrified :value} result)))))

      (testing "when saving to the repository throws"
        (stubs/use! tx :execute!
                    (ex-info "kaboom!" {}))
        (testing "fails"
          (is (thrown? Throwable (int/create! repo {:user/id user-id})))))

      (testing "when querying the repository throws"
        (stubs/use! tx :execute!
                    [{:id project-id}]
                    (ex-info "kaboom!" {}))
        (testing "fails"
          (is (thrown? Throwable (int/create! repo {:user/id user-id}))))))))
