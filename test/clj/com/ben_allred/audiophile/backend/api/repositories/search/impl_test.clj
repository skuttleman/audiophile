(ns ^:unit com.ben-allred.audiophile.backend.api.repositories.search.impl-test
  (:require
    [clojure.test :refer [are deftest is testing]]
    [com.ben-allred.audiophile.backend.api.repositories.search.impl :as rsearch]
    [com.ben-allred.audiophile.backend.domain.interactors.core :as int]
    [com.ben-allred.audiophile.common.core.utils.colls :as colls]
    [com.ben-allred.audiophile.test.utils :as tu]
    [com.ben-allred.audiophile.test.utils.repositories :as trepos]
    [com.ben-allred.audiophile.test.utils.stubs :as stubs]))

(deftest exists?-test
  (testing "exists?"
    (let [tx (trepos/stub-transactor trepos/->user-executor)
          repo (rsearch/->SearchAccessor tx)]
      (testing "when searching by handle"
        (testing "and when the handle is found"
          (stubs/init! tx)
          (stubs/use! tx :execute!
                      [{}])
          (let [result (int/exists? repo {:search/field :user/handle
                                          :search/value "handle"})
                [stuff] (colls/only! (stubs/calls tx :execute!))]
            (is (= {:select #{1}
                    :from   [:users]
                    :where  [:= #{:users.handle "handle"}]}
                   (-> stuff
                       (update :select set)
                       (update :where tu/op-set))))
            (is (true? result))))

        (testing "and when the handle is not found"
          (stubs/init! tx)
          (stubs/use! tx :execute!
                      [])
          (let [result (int/exists? repo {:search/field :user/handle
                                          :search/value "handle"})]
            (is (false? result))))

        (testing "and when the repository throws"
          (stubs/init! tx)
          (stubs/use! tx :execute!
                      (ex-info "Executor" {}))
          (is (thrown? Throwable
                       (int/exists? repo {:search/field :user/handle
                                          :search/value "handle"})))))

      (testing "when searching by mobile-number"
        (testing "and when the mobile-number is found"
          (stubs/init! tx)
          (stubs/use! tx :execute!
                      [{}])
          (let [result (int/exists? repo {:search/field :user/mobile-number
                                          :search/value "mobile-number"})
                [stuff] (colls/only! (stubs/calls tx :execute!))]
            (is (= {:select #{1}
                    :from   [:users]
                    :where  [:= #{:users.mobile-number "mobile-number"}]}
                   (-> stuff
                       (update :select set)
                       (update :where tu/op-set))))
            (is (true? result))))

        (testing "and when the mobile-number is not found"
          (stubs/init! tx)
          (stubs/use! tx :execute!
                      [])
          (let [result (int/exists? repo {:search/field :user/mobile-number
                                          :search/value "mobile-number"})]
            (is (false? result))))

        (testing "and when the repository throws"
          (stubs/init! tx)
          (stubs/use! tx :execute!
                      (ex-info "Executor" {}))
          (is (thrown? Throwable
                       (int/exists? repo {:search/field :user/mobile-number
                                          :search/value "mobile-number"}))))))))
