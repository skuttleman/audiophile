(ns ^:unit audiophile.backend.infrastructure.repositories.users.impl-test
  (:require
    [audiophile.backend.domain.interactors.core :as int]
    [audiophile.backend.infrastructure.repositories.users.impl :as rusers]
    [audiophile.backend.infrastructure.templates.workflows :as wf]
    [audiophile.common.core.utils.colls :as colls]
    [audiophile.common.core.utils.uuids :as uuids]
    [audiophile.test.utils :as tu]
    [audiophile.test.utils.assertions :as assert]
    [audiophile.test.utils.services :as ts]
    [audiophile.test.utils.stubs :as stubs]
    [clojure.test :refer [are deftest is testing]]
    [spigot.controllers.kafka.topologies :as sp.ktop]))

(deftest query-by-email-test
  (testing "query-by-email"
    (let [tx (ts/->tx)
          repo (rusers/->UserAccessor tx nil)
          user-id (uuids/random)]
      (testing "when querying for a user"
        (stubs/use! tx :execute!
                    [{:id user-id :some :details}])
        (let [result (int/query-one repo {:user/email "user@domain.tld"})]
          (testing "sends the query to the repository"
            (let [[{:keys [from select where]}] (colls/only! (stubs/calls tx :execute!))]
              (is (= #{[:users.id "user/id"]
                       [:users.first-name "user/first-name"]
                       [:users.handle "user/handle"]}
                     (set select)))
              (is (= [:users] from))
              (is (= [:= #{:users.email "user@domain.tld"}]
                     (tu/op-set where)))))

          (testing "returns the result"
            (is (= {:id user-id :some :details} result)))))

      (testing "when the repository throws"
        (stubs/use! tx :execute!
                    (ex-info "Nope" {}))
        (testing "fails"
          (is (thrown? Throwable (int/query-one repo {:user/email "user@domain.tld"}))))))))

(deftest query-by-id-test
  (testing "query-by-id"
    (let [tx (ts/->tx)
          repo (rusers/->UserAccessor tx nil)
          user-id (uuids/random)]
      (testing "when querying for a user"
        (stubs/use! tx :execute!
                    [{:id user-id :some :details}])
        (let [result (int/query-one repo {:user/id user-id})]
          (testing "sends the query to the repository"
            (let [[{:keys [from select where]}] (colls/only! (stubs/calls tx :execute!))]
              (is (= #{[:users.id "user/id"]
                       [:users.handle "user/handle"]
                       [:users.email "user/email"]
                       [:users.first-name "user/first-name"]
                       [:users.last-name "user/last-name"]
                       [:users.mobile-number "user/mobile-number"]
                       [:users.created-at "user/created-at"]}
                     (set select)))
              (is (= [:users] from))
              (is (= [:= #{:users.id user-id}]
                     (tu/op-set where)))))

          (testing "returns the result"
            (is (= {:id user-id :some :details} result)))))

      (testing "when the repository throws"
        (stubs/use! tx :execute!
                    (ex-info "Nope" {}))
        (testing "fails"
          (is (thrown? Throwable (int/query-one repo {:user/id user-id}))))))))

(deftest create!-test
  (testing "create!"
    (let [producer (ts/->producer)
          repo (rusers/->UserAccessor nil producer)
          user-id (uuids/random)]
      (testing "emits a command"
        (int/create! repo
                     {:user/handle        "handle"
                      :user/email         "email"
                      :user/first-name    "first"
                      :user/last-name     "last"
                      :user/mobile-number "mobile"}
                     {:user/id user-id})
        (let [[_ [tag params ctx]] (colls/only! (stubs/calls producer :send!))]
          (is (= ::sp.ktop/create! tag))
          (assert/is? {:workflows/ctx      '{?handle        "handle"
                                             ?email         "email"
                                             ?first-name    "first"
                                             ?last-name     "last"
                                             ?mobile-number "mobile"}
                       :workflows/template :users/signup
                       :workflows/form     (peek (wf/load! :users/signup))
                       :workflows/->result {:login/token '(sp.ctx/get ?token)}}
                      params)
          (assert/is? {:user/id user-id
                       :workflow/id uuid?}
                      ctx))))))
