(ns ^:unit com.ben-allred.audiophile.api.services.interactors.users.core-test
  (:require
    [clojure.test :refer [are deftest is testing]]
    [com.ben-allred.audiophile.api.services.interactors.core :as int]
    [com.ben-allred.audiophile.api.services.repositories.users.core :as rusers]
    [com.ben-allred.audiophile.common.utils.colls :as colls]
    [com.ben-allred.audiophile.common.utils.uuids :as uuids]
    [test.utils.repositories :as trepos]
    [test.utils.stubs :as stubs]
    [test.utils :as tu]))

(deftest query-by-email-test
  (testing "query-by-email"
    (let [tx (trepos/stub-transactor)
          repo (rusers/->UserAccessor tx)
          user-id (uuids/random)]
      (testing "when querying for a user"
        (stubs/use! tx :execute!
                    [{:id user-id :some :details}])
        (let [result (int/query-one repo {:user/email "user@domain.tld"})]
          (testing "sends the query to the repository"
            (let [[{:keys [from select where]}] (colls/only! (stubs/calls tx :execute!))]
              (is (= #{[:users.id "user/id"]
                       [:users.first-name "user/first-name"]
                       [:users.last-name "user/last-name"]
                       [:users.handle "user/handle"]
                       [:users.email "user/email"]
                       [:users.created-at "user/created-at"]}
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