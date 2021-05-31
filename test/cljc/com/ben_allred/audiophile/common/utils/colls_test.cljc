(ns com.ben-allred.audiophile.common.utils.colls-test
  (:require
    [clojure.test :refer [are deftest is testing]]
    [com.ben-allred.audiophile.common.utils.colls :as colls]))

(deftest cons?-test
  (testing "produces the expected output"
    (are [input expected] (= expected (colls/cons? input))
      () true
      (range) true
      (lazy-seq) true
      [] false
      {} false
      nil false
      "string" false
      1 false)))

(deftest force-sequential-test
  (testing "produces the expected output"
    (are [input expected] (= expected (colls/force-sequential input))
      () ()
      [] []
      #{} [#{}]
      {} [{}]
      nil [nil]
      "string" ["string"]
      1 [1])))

(deftest only!-test
  (testing "produces the expected output"
    (are [input expected] (= expected (colls/only! input))
      [] nil
      [nil] nil
      (list :foo) :foo
      [{:a :thing}] {:a :thing})

    (are [n input expected] (= expected (colls/only! n input))
      1 [1] [1]
      3 [1] [1]
      3 [1 2 3] [1 2 3]
      0 [] [])

    (are [input] (thrown? #?(:cljs :default :default Throwable) (colls/only! input))
      [1 2]
      (range 10)
      {:a 1 :b 2}
      #{:a :b :c})

    (are [n input] (thrown? #?(:cljs :default :default Throwable) (colls/only! n input))
      1 [1 2]
      3 [1 2 3 4]
      0 [nil])))
