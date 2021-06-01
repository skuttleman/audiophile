(ns ^:unit com.ben-allred.audiophile.common.core.utils.maps-test
  (:require
    [clojure.test :refer [are deftest is testing]]
    [com.ben-allred.audiophile.common.core.utils.maps :as maps]))

(deftest update-maybe-test
  (testing "when the value of `k` is not nil"
    (testing "updates the map"
      (is (= {:a 8} (maps/update-maybe {:a 1} :a + 7)))))

  (testing "when the value of `k` is nil"
    (testing "does not update the map"
      (is (= {:a 1} (maps/update-maybe {:a 1} :b + 7))))))

(deftest update-in-maybe-test
  (testing "when the value of `k` is not nil"
    (testing "updates the map"
      (is (= {:m {:a 8}} (maps/update-in-maybe {:m {:a 1}} [:m :a] + 7)))))

  (testing "when the value of `k` is nil"
    (testing "does not update the map"
      (is (= {:m {:a 1}} (maps/update-in-maybe {:m {:a 1}} [:m :b] + 7))))))

(deftest assoc-defaults-test
  (testing "when the value of `k` is not in the map"
    (testing "updates the map"
      (is (= {:a 1 :b 2} (maps/assoc-defaults {:a 1} :a 7 :b 2)))))

  (testing "when the value of `k` is nil"
    (testing "does not update the map"
      (is (= {:a 1 :b 0} (maps/assoc-defaults  {:a 1 :b 0} :a 7 :b 7))))))

(deftest assoc-maybe-test
  (testing "when the value being added is not nil"
    (is (= {:a 1 :b 7} (maps/assoc-maybe {:a 1} :b 7))))

  (testing "when the value being added is nil"
    (is (= {:a 1} (maps/assoc-maybe {:a 1} :b nil)))))

(deftest assoc-in-maybe-test
  (testing "when the value being added is not nil"
    (is (= {:m {:a 1 :b 7}} (maps/assoc-in-maybe {:m {:a 1}} [:m :b] 7))))

  (testing "when the value being added is nil"
    (is (= {:m {:a 1}} (maps/assoc-in-maybe {:m {:a 1}} [:m :b] nil)))))

(deftest dissocp-test
  (testing "removes all keys that satisfy `pred`"
    (is (= {:b 2 :d 4} (maps/dissocp {:a 1 :b 2 :c 3 :d 4} odd?))))

  (testing "retains metadata"
    (is (= {:some :meta} (meta (maps/dissocp ^{:some :meta} {:a 1 :b 2} odd?))))))

(deftest flatten-test
  (testing "flattens a nested map into a map of paths into a leaf value associated with the value"
    (is (= {[:a :b] 1 [:c] 2 [:d :e :f] [1 2 3]}
           (maps/flatten {:a {:b 1} :c 2 :d {:e {:f [1 2 3]}}})))))

(deftest nest-test
  (testing "nests a flattened map of path -> leaf value"
    (is (= {:a {:b 1} :c 2 :d {:e {:f [1 2 3]}}}
           (maps/nest {[:a :b] 1 [:c] 2 [:d :e :f] [1 2 3]})))))

(deftest deep-merge-test
  (testing "merged nested maps"
    (is (= {:a {:b 1 :c {:d :e} :f [:g :h]} :i {:j :k}}
           (maps/deep-merge {:a {:b 1 :f [:g]} :i {:j nil}}
                            {:a {:c {:d :e} :f [:g :h]} :i {:j :k}})))))

(deftest map-keys-test
  (testing "maps `f` over the keys of m"
    (is (= {2 :a 3 :b}
           (maps/map-keys inc {1 :a 2 :b})))))

(deftest map-vals-test
  (testing "maps `f` over the keys of m"
    (is (= {:a 0 :b 1}
           (maps/map-vals dec {:a 1 :b 2})))))

(deftest ->m-test
  (testing "compiles a sequence of items into a map"
    (is (= {:a 1 :b :B :c 3}
           (let [a 1
                 b :B
                 three 3]
             (maps/->m a b [:c three]))))))

(deftest extract-keys-test
  (testing "separates a map into two maps split on a set of keys"
    (is (= [{:a 1 :b 2} {:c 3 :d 4}]
           (maps/extract-keys {:a 1 :b 2 :c 3 :d 4} #{:a :b})))))
