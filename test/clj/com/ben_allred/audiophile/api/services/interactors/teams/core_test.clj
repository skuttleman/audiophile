(ns ^:unit com.ben-allred.audiophile.api.services.interactors.teams.core-test
  (:require
    [clojure.test :refer [are deftest is testing]]
    [com.ben-allred.audiophile.api.services.interactors.core :as int]
    [com.ben-allred.audiophile.api.services.repositories.teams.core :as rteams]
    [com.ben-allred.audiophile.api.services.repositories.teams.queries :as qteams]
    [com.ben-allred.audiophile.common.utils.colls :as colls]
    [com.ben-allred.audiophile.common.utils.fns :as fns]
    [com.ben-allred.audiophile.common.utils.uuids :as uuids]
    [test.utils :as tu]
    [test.utils.repositories :as trepos]
    [test.utils.stubs :as stubs]))

(defn ^:private ->team-executor [executor {:keys [teams user-teams users]}]
  (qteams/->TeamExecutor executor teams user-teams users))

(deftest query-all-test
  (testing "query-all"
    (let [tx (trepos/stub-transactor ->team-executor)
          repo (rteams/->TeamAccessor tx)
          user-id (uuids/random)]
      (testing "when querying teams"
        (stubs/use! tx :execute!
                    [{:some :results}
                     {:for :you}])
        (let [result (int/query-many repo {:user/id user-id})
              [{:keys [from select where]}] (colls/only! (stubs/calls tx :execute!))]
          (testing "sends a query to the repository"
            (is (= #{[:teams.name "team/name"]
                     [:teams.type "team/type"]
                     [:teams.created-by "team/created-by"]
                     [:teams.id "team/id"]
                     [:teams.created-at "team/created-at"]}
                   (set select)))
            (is (= [:teams] from))
            (is (= [:exists {:select #{1}
                             :from   [:user-teams]
                             :where  [:and
                                      #{[:= #{:user-teams.team-id :teams.id}]
                                        [:= #{:user-teams.user-id user-id}]}]}]
                   (-> where
                       (update 1 (fns/=> (update :select set)
                                         (update :where (fns/=> (update 1 tu/op-set)
                                                                (update 2 tu/op-set)
                                                                tu/op-set))))))))

          (testing "returns the results"
            (is (= [{:some :results} {:for :you}] result)))))

      (testing "when querying the repository throws"
        (stubs/use! tx :execute!
                    (ex-info "Bad" {}))
        (testing "fails")))))

(deftest query-by-id-test
  (testing "query-by-id"
    (let [tx (trepos/stub-transactor ->team-executor)
          repo (rteams/->TeamAccessor tx)
          [team-id user-id] (repeatedly uuids/random)]
      (testing "when querying teams"
        (stubs/use! tx :execute!
                    [{:some :result}]
                    [{:with :team}])
        (let [result (int/query-one repo {:user/id user-id
                                          :team/id team-id})
              [[select-team] [select-members]] (colls/only! 2 (stubs/calls tx :execute!))]
          (testing "queries the team from the repository"
            (let [{:keys [from select where]} select-team]
              (is (= #{[:teams.name "team/name"]
                       [:teams.type "team/type"]
                       [:teams.created-by "team/created-by"]
                       [:teams.id "team/id"]
                       [:teams.created-at "team/created-at"]}
                     (set select)))
              (is (= [:teams] from))
              (let [[clause & clauses] where
                    clauses' (into {} (map (juxt tu/op-set identity)) clauses)]
                (is (= :and clause))
                (is (contains? clauses' [:= #{:teams.id team-id}]))
                (is (= [:exists {:select #{1}
                                 :from   [:user-teams]
                                 :where  [:and
                                          #{[:= #{:user-teams.team-id :teams.id}]
                                            [:= #{:user-teams.user-id user-id}]}]}]
                       (-> clauses'
                           (dissoc [:= #{:teams.id team-id}])
                           colls/only!
                           val
                           (update 1 (fns/=> (update :select set)
                                             (update :where (fns/=> (update 1 tu/op-set)
                                                                    (update 2 tu/op-set)
                                                                    tu/op-set))))))))))

          (testing "queries the team members from the repository"
            (let [{:keys [from select where]} select-members]
              (is (= #{[:member.id "member/id"]
                       [:member.first-name "member/first-name"]
                       [:member.last-name "member/last-name"]
                       [:user-teams.team-id "member/team-id"]}
                     (set select)))
              (is (= [[:users :member]] from))
              (is (= [:= #{:user-teams.team-id team-id}]
                     (tu/op-set where)))))


          (testing "returns the results"
            (is (= {:some :result :team/members [{:with :team}]} result)))))

      (testing "when querying the repository throws"
        (stubs/use! tx :execute!
                    (ex-info "Bad" {}))
        (testing "fails")))))

(deftest create!-test
  (testing "create!"
    (let [tx (trepos/stub-transactor ->team-executor)
          repo (rteams/->TeamAccessor tx)
          [team-id user-id] (repeatedly uuids/random)]
      (testing "when creating a team"
        (stubs/use! tx :execute!
                    [{:id team-id}]
                    [{:user :team}]
                    [{:transmogrified :value}])
        (let [result (int/create! repo
                                  {:created-at :whenever
                                   :other      :junk
                                   :user/id    user-id})
              [[insert-team] [insert-user-team] [select]] (colls/only! 3 (stubs/calls tx :execute!))]
          (testing "saves to the repository"
            (is (= {:insert-into :teams
                    :values      [{:created-at :whenever
                                   :created-by user-id}]
                    :returning   [:id]}
                   insert-team))
            (is (= {:insert-into :user-teams
                    :values      [{:user-id user-id
                                   :team-id team-id}]
                    :returning   [:*]}
                   insert-user-team)))

          (testing "queries from the repository"
            (is (= {:select #{[:teams.created-at "team/created-at"]
                              [:teams.name "team/name"]
                              [:teams.type "team/type"]
                              [:teams.id "team/id"]
                              [:teams.created-by "team/created-by"]}
                    :from   [:teams]
                    :where  [:= #{:teams.id team-id}]}
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
          (is (thrown? Throwable (int/create! tx {:user/id user-id})))))

      (testing "when querying the repository throws"
        (stubs/use! tx :execute!
                    [{:id team-id}]
                    []
                    (ex-info "kaboom!" {}))
        (testing "fails"
          (is (thrown? Throwable (int/create! tx {:user/id user-id}))))))))
