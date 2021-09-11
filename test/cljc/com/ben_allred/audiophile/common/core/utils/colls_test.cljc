(ns ^:unit com.ben-allred.audiophile.common.core.utils.colls-test
  (:require
    [clojure.test :refer [are deftest is testing]]
    [com.ben-allred.audiophile.common.core.utils.colls :as colls]))

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

(deftest nest-children-test
  (testing "produces the expected output"
    (let [coll [{:id  1
                 :rev 10}
                {:id  2
                 :rev 9}
                {:id  3
                 :rev 8}
                {:id        4
                 :parent-id 2
                 :rev       7}
                {:id        5
                 :parent-id 4
                 :rev       6}
                {:id        6
                 :parent-id 2
                 :rev       5}
                {:id  7
                 :rev 4}
                {:id        8
                 :parent-id 1
                 :rev       3}
                {:id        9
                 :parent-id 8
                 :rev       2}
                {:id        10
                 :parent-id 4
                 :rev       1}]]
      (is (= [{:id   1
               :rev  10
               :nest [{:id        8
                       :parent-id 1
                       :rev       3
                       :nest      [{:id        9
                                    :parent-id 8
                                    :rev       2}]}]}
              {:id   2
               :rev  9
               :nest [{:id        4
                       :parent-id 2
                       :rev       7
                       :nest      [{:id        5
                                    :parent-id 4
                                    :rev       6}
                                   {:id        10
                                    :parent-id 4
                                    :rev       1}]}
                      {:id        6
                       :parent-id 2
                       :rev       5}]}
              {:id  3
               :rev 8}
              {:id  7
               :rev 4}]
             (colls/nest-children :id :parent-id :nest coll)))
      (is (= [{:id  7
               :rev 4}
              {:id  3
               :rev 8}
              {:id   2
               :rev  9
               :nest [{:id        6
                       :parent-id 2
                       :rev       5}
                      {:id        4
                       :parent-id 2
                       :rev       7
                       :nest      [{:id        10
                                    :parent-id 4
                                    :rev       1}
                                   {:id        5
                                    :parent-id 4
                                    :rev       6}]}]}
              {:id   1
               :rev  10
               :nest [{:id        8
                       :parent-id 1
                       :rev       3
                       :nest      [{:id        9
                                    :parent-id 8
                                    :rev       2}]}]}]
             (colls/nest-children :id :parent-id :nest (reverse coll)))))))
