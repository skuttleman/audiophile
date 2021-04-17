(ns com.ben-allred.audiophile.common.core-test
  (:require
    [clojure.test :refer [are deftest is testing]]))

(deftest my-test
  (testing "3 + 3 == 6"
    (is (= 6 (+ 3 3)))))
