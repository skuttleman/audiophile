(ns ^:unit audiophile.backend.infrastructure.repositories.team-invitations.impl-test
  (:require
    [audiophile.backend.domain.interactors.core :as int]
    [audiophile.backend.infrastructure.repositories.team-invitations.impl :as rinvitations]
    [audiophile.backend.infrastructure.repositories.teams.impl :as rteams]
    [audiophile.backend.infrastructure.templates.workflows :as wf]
    [audiophile.common.core.utils.colls :as colls]
    [audiophile.common.core.utils.uuids :as uuids]
    [audiophile.test.utils.assertions :as assert]
    [audiophile.test.utils.services :as ts]
    [audiophile.test.utils.stubs :as stubs]
    [clojure.test :refer [are deftest is testing]]
    [spigot.controllers.kafka.topologies :as sp.ktop]))

(deftest create!-test
  (testing "create!"
    (let [producer (ts/->chan)
          tx (ts/->tx)
          repo (rinvitations/->TeamInvitationAccessor tx producer)
          [inviter-id request-id team-id user-id] (repeatedly uuids/random)]
      (testing "when the user has access"
        (stubs/use! tx :execute!
                    [{}]
                    [{:user/id user-id}])
        (testing "emits a command"
          (int/create! repo {:some :data} {:some       :opts
                                           :some/other :opts
                                           :team/id    team-id
                                           :user/id    inviter-id
                                           :request/id request-id})
          (let [[{[tag params ctx] :value}] (colls/only! (stubs/calls producer :send!))]
            (is (= ::sp.ktop/create! tag))
            (assert/is? {:workflows/ctx      {'?user-id    user-id
                                              '?inviter-id inviter-id
                                              '?team-id    team-id}
                         :workflows/template :team-invitations/create
                         :workflows/form     (peek (wf/load! :team-invitations/create))}
                        params)
            (assert/is? {:user/id     inviter-id
                         :request/id  request-id
                         :workflow/id uuid?}
                        ctx))))

      (testing "when the user does not have access"
        (stubs/use! tx :execute! [])
        (testing "throws"
          (let [ex (is (thrown? Throwable (int/create! repo {:some :data} {:some       :opts
                                                                           :some/other :opts
                                                                           :team/id    team-id
                                                                           :user/id    user-id
                                                                           :request/id request-id})))]
            (is (= int/NO_ACCESS (:interactor/reason (ex-data ex))))))))))

(deftest update!-test
  (testing "update!"
    (let [producer (ts/->chan)
          tx (ts/->tx)
          repo (rinvitations/->TeamInvitationAccessor tx producer)
          [inviter-id request-id team-id user-id] (repeatedly uuids/random)]
      (testing "when the user has access"
        (stubs/use! tx :execute!
                    [{:team-invitation/invited-by inviter-id}])
        (testing "emits a command"
          (int/update! repo
                       {:team-invitation/team-id team-id}
                       {:some       :opts
                        :some/other :opts
                        :team/id    team-id
                        :user/id    user-id
                        :request/id request-id})
          (let [[{[tag params ctx] :value}] (colls/only! (stubs/calls producer :send!))]
            (is (= ::sp.ktop/create! tag))
            (assert/is? {:workflows/ctx      {'?user-id    user-id
                                              '?inviter-id inviter-id
                                              '?team-id    team-id}
                         :workflows/template :team-invitations/update
                         :workflows/form     (peek (wf/load! :team-invitations/update))}
                        params)
            (assert/is? {:user/id     user-id
                         :request/id  request-id
                         :workflow/id uuid?}
                        ctx))))

      (testing "when the user does not have access"
        (stubs/use! tx :execute! [])
        (testing "throws"
          (let [ex (is (thrown? Throwable (int/create! repo {:some :data} {:some       :opts
                                                                           :some/other :opts
                                                                           :team/id    team-id
                                                                           :user/id    user-id
                                                                           :request/id request-id})))]
            (is (= int/NO_ACCESS (:interactor/reason (ex-data ex))))))))))
