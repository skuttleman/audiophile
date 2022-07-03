(ns ^:unit audiophile.backend.infrastructure.repositories.teams.impl-test
  (:require
    [audiophile.backend.domain.interactors.core :as int]
    [audiophile.backend.infrastructure.repositories.teams.impl :as rteams]
    [audiophile.backend.infrastructure.templates.workflows :as wf]
    [audiophile.common.core.utils.colls :as colls]
    [audiophile.common.core.utils.fns :as fns]
    [audiophile.common.core.utils.uuids :as uuids]
    [audiophile.test.utils :as tu]
    [audiophile.test.utils.assertions :as assert]
    [audiophile.test.utils.repositories :as trepos]
    [audiophile.test.utils.services :as ts]
    [audiophile.test.utils.stubs :as stubs]
    [clojure.test :refer [are deftest is testing]]
    [spigot.controllers.kafka.topologies :as sp.ktop]))

(deftest query-all-test
  (testing "query-all"
    (let [tx (trepos/stub-transactor)
          repo (rteams/->TeamAccessor tx nil)
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
    (let [tx (trepos/stub-transactor)
          repo (rteams/->TeamAccessor tx nil)
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
    (let [producer (ts/->producer)
          repo (rteams/->TeamAccessor nil producer)
          [user-id request-id] (repeatedly uuids/random)]
      (testing "emits a command"
        (int/create! repo {:some :data} {:some       :opts
                                         :some/other :opts
                                         :user/id    user-id
                                         :request/id request-id})
        (let [[_ [tag params ctx]] (colls/only! (stubs/calls producer :send!))]
          (is (= ::sp.ktop/create! tag))
          (assert/is? {:workflows/ctx      {'?user-id user-id}
                       :workflows/template :teams/create
                       :workflows/form     (peek (wf/load! :teams/create))
                       :workflows/->result {:team/id '(sp.ctx/get ?team-id)}}
                      params)
          (assert/is? {:user/id     user-id
                       :request/id  request-id
                       :workflow/id uuid?}
                      ctx))))))
