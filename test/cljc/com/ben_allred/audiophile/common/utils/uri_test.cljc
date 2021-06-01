(ns ^:unit com.ben-allred.audiophile.common.utils.uri-test
  (:require
    [clojure.test :refer [are deftest is testing]]
    [com.ben-allred.audiophile.common.utils.uri :as uri]))

(deftest join-query-test
  (testing "joins a map into a query-string"
    (are [input expected] (= expected (uri/join-query input))
      nil nil
      {} nil
      {:a 1} "a=1"
      {:a 1 :b true} "a=1&b"
      {:a 1 :b false} "a=1"
      {:a [1 2 3] :b :thing} "a=1&a=2&a=3&b=thing")))

(deftest split-query-test
  (testing "joins a map into a query-string"
    (are [input expected] (= expected (uri/split-query input))
      nil nil
      "a=1" {:a "1"}
      "a=1&b" {:a "1" :b true}
      "a=1&a=2&a=3&b=thing" {:a ["1" "2" "3"] :b "thing"})))
