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
    [audiophile.test.utils.services :as ts]
    [audiophile.test.utils.stubs :as stubs]
    [clojure.test :refer [are deftest is testing]]
    [spigot.controllers.kafka.topologies :as sp.ktop]))

(deftest query-all-test
  (testing "query-all"
    (let [tx (ts/->tx)
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
    (let [tx (ts/->tx)
          repo (rteams/->TeamAccessor tx nil)
          [team-id user-id] (repeatedly uuids/random)]
      (testing "when querying teams"
        (stubs/use! tx :execute!
                    [{:some :result}]
                    [{:with :team}]
                    [{:with :project}]
                    [{:with :invitations}])
        (let [result
              (int/query-one repo {:user/id user-id
                                   :team/id team-id})

              [[select-team] [select-members] [select-projects] [select-invitations]]
              (colls/only! 4 (stubs/calls tx :execute!))]
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

          (testing "queries the team projects from the repository"
            (let [{:keys [from select where]} select-projects]
              (is (= #{[:projects.created-at "project/created-at"]
                       [:projects.id "project/id"]
                       [:projects.name "project/name"]
                       [:projects.team-id "project/team-id"]}
                     (set select)))
              (is (= [:projects] from))
              (is (= [:= #{:projects.team-id team-id}]
                     (tu/op-set where)))))

          (testing "queries the team invitations from the repository"
            (let [{:keys [from join select where]} select-invitations]
              (is (= #{[:inviter.id "inviter/id"]
                       [:inviter.first-name "inviter/first-name"]
                       [:inviter.last-name "inviter/last-name"]
                       [:team-invitations.email "team-invitation/email"]
                       [:team-invitations.created-at "team-invitation/created-at"]}
                     (set select)))
              (is (= [:team-invitations] from))
              (is (= [[:users :inviter] [:= #{:inviter.id :team-invitations.invited-by}]]
                     (update join 1 tu/op-set)))
              (is (= [:and
                      #{[:= #{:team-invitations.team-id team-id}]
                        [:= #{:team-invitations.status [:cast "PENDING" :team-invitation-status]}]}]
                     (-> where
                         (update 1 tu/op-set)
                         (update 2 tu/op-set)
                         tu/op-set)))))

          (testing "returns the results"
            (is (= {:some          :result
                    :team/members  [{:with :team}]
                    :team/projects [{:with :project}]
                    :team/invitations [{:with :invitations}]}
                   result)))))

      (testing "when querying the repository throws"
        (stubs/use! tx :execute!
                    (ex-info "Bad" {}))
        (testing "fails")))))

(deftest create!-test
  (testing "create!"
    (let [producer (ts/->chan)
          tx (ts/->tx)
          repo (rteams/->TeamAccessor tx producer)
          [request-id user-id] (repeatedly uuids/random)]
      (testing "when the user has access"
        (stubs/use! tx :execute! [{}])
        (testing "emits a command"
          (int/create! repo {:some :data} {:some       :opts
                                           :some/other :opts
                                           :user/id    user-id
                                           :request/id request-id})
          (let [[{[tag params ctx] :value}] (colls/only! (stubs/calls producer :send!))]
            (is (= ::sp.ktop/create! tag))
            (assert/is? {:workflows/ctx      {'?user-id user-id}
                         :workflows/template :teams/create
                         :workflows/form     (peek (wf/load! :teams/create))
                         :workflows/->result {:team/id '(spigot/get ?team-id)}}
                        params)
            (assert/is? {:user/id     user-id
                         :request/id  request-id
                         :workflow/id uuid?}
                        ctx)))))))

(deftest update!-test
  (testing "update!"
    (let [producer (ts/->chan)
          tx (ts/->tx)
          repo (rteams/->TeamAccessor tx producer)
          [request-id team-id user-id] (repeatedly uuids/random)]
      (testing "when the user has access"
        (stubs/use! tx :execute! [{}])
        (testing "emits a command"
          (int/update! repo {:some :data} {:some       :opts
                                           :some/other :opts
                                           :team/id    team-id
                                           :user/id    user-id
                                           :request/id request-id})
          (let [[{[tag params ctx] :value}] (colls/only! (stubs/calls producer :send!))]
            (is (= ::sp.ktop/create! tag))
            (assert/is? {:workflows/ctx      {'?team-id team-id}
                         :workflows/template :teams/update
                         :workflows/form     (peek (wf/load! :teams/update))}
                        params)
            (assert/is? {:user/id     user-id
                         :request/id  request-id
                         :workflow/id uuid?}
                        ctx))))

      (testing "when the user does not have access"
        (stubs/use! tx :execute! [])
        (testing "throws"
          (let [ex (is (thrown? Throwable (int/update! repo {:some :data} {:some       :opts
                                                                           :some/other :opts
                                                                           :team/id    team-id
                                                                           :user/id    user-id
                                                                           :request/id request-id})))]
            (is (= int/NO_ACCESS (:interactor/reason (ex-data ex))))))))))
