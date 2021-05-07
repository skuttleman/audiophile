(ns ^:unit com.ben-allred.audiophile.api.services.repositories.teams.model-test
  (:require
    [clojure.test :refer [are deftest is testing]]
    [com.ben-allred.audiophile.api.services.repositories.teams.model :as teams]
    [com.ben-allred.audiophile.common.utils.colls :as colls]
    [com.ben-allred.audiophile.common.utils.uuids :as uuids]
    [test.utils.mocks :as mocks]
    [test.utils.repositories :as trepos]
    [com.ben-allred.audiophile.common.utils.fns :as fns]
    [test.utils :as tu]))

(deftest query-all-test
  (testing "query-all"
    (let [tx (trepos/mock-transactor)
          user-id (uuids/random)]
      (testing "when querying teams"
        (mocks/use! tx :execute!
                    [{:some :results}
                     {:for :you}])
        (let [result (teams/query-all tx user-id)
              [{:keys [from select where]}] (colls/only! (mocks/calls tx :execute!))]
          (testing "sends a query to the repository"
            (is (= #{[:teams.name "team/name"]
                     [:teams.type "team/type"]
                     [:teams.created-by "team/created-by"]
                     [:teams.id "team/id"]
                     [:teams.created-at "team/created-at"]}
                   (set select)))
            (is (= [:teams] from))
            (is (= [:exists {:select #{:user-id}
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
        (mocks/use! tx :execute!
                    (ex-info "Bad" {}))
        (testing "fails")))))

(deftest query-by-id-test
  (testing "query-by-id"
    (let [tx (trepos/mock-transactor)
          [team-id user-id] (repeatedly uuids/random)]
      (testing "when querying teams"
        (mocks/use! tx :execute!
                    [{:some :result}]
                    [{:with :team}])
        (let [result (teams/query-by-id tx team-id user-id)
              [[select-team] [select-members]] (colls/only! 2 (mocks/calls tx :execute!))]
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
                (is (= [:exists {:select #{:user-id}
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
        (mocks/use! tx :execute!
                    (ex-info "Bad" {}))
        (testing "fails")))))

(deftest create!-test
  (testing "create!"
    (let [tx (trepos/mock-transactor)
          [team-id user-id] (repeatedly uuids/random)]
      (testing "when creating a team"
        (mocks/use! tx :execute!
                    [{:id team-id}]
                    [{:user :team}]
                    [{:transmogrified :value}])
        (let [result (teams/create! tx
                                    {:created-at :whenever :other :junk}
                                    user-id)
              [[insert-team] [insert-user-team] [select]] (colls/only! 3 (mocks/calls tx :execute!))]
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
        (mocks/use! tx :execute!
                    (ex-info "kaboom!" {}))
        (testing "fails"
          (is (thrown? Throwable (teams/create! tx {} user-id)))))

      (testing "when querying the repository throws"
        (mocks/use! tx :execute!
                    [{:id team-id}]
                    []
                    (ex-info "kaboom!" {}))
        (testing "fails"
          (is (thrown? Throwable (teams/create! tx {} user-id))))))))

(comment
  (clojure.test/run-tests))
