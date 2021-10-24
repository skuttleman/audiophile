(ns ^:unit com.ben-allred.audiophile.backend.api.repositories.users.impl-test
  (:require
    [clojure.test :refer [are deftest is testing]]
    [com.ben-allred.audiophile.backend.api.repositories.users.impl :as rusers]
    [com.ben-allred.audiophile.backend.domain.interactors.core :as int]
    [com.ben-allred.audiophile.common.core.utils.colls :as colls]
    [com.ben-allred.audiophile.common.core.utils.uuids :as uuids]
    [com.ben-allred.audiophile.test.utils :as tu]
    [com.ben-allred.audiophile.test.utils.assertions :as assert]
    [com.ben-allred.audiophile.test.utils.repositories :as trepos]
    [com.ben-allred.audiophile.test.utils.services :as ts]
    [com.ben-allred.audiophile.test.utils.stubs :as stubs]))

(deftest query-by-email-test
  (testing "query-by-email"
    (let [tx (trepos/stub-transactor trepos/->user-executor)
          repo (rusers/->UserAccessor tx nil)
          user-id (uuids/random)]
      (testing "when querying for a user"
        (stubs/use! tx :execute!
                    [{:id user-id :some :details}])
        (let [result (int/query-one repo {:user/email "user@domain.tld"})]
          (testing "sends the query to the repository"
            (let [[{:keys [from select where]}] (colls/only! (stubs/calls tx :execute!))]
              (is (= #{[:users.id "user/id"]}
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

(deftest create!-test
  (testing "create!"
    (let [ch (ts/->chan)
          repo (rusers/->UserAccessor nil ch)
          user-id (uuids/random)]
      (int/create! repo {:some :data} {:user/id user-id})
      (testing "emits a command"
        (assert/is? {:command/id         uuid?
                     :command/type       :user/create!
                     :command/data       {:some :data}
                     :command/emitted-by user-id
                     :command/ctx        {:user/id user-id}}
                    (first (colls/only! (stubs/calls ch :send!))))))))
