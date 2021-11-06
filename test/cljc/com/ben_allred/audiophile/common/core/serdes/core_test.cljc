(ns ^:unit com.ben-allred.audiophile.common.core.serdes.core-test
  (:require
    [clojure.test :refer [are deftest is testing]]
    [com.ben-allred.audiophile.common.core.serdes.core :as serdes]
    [com.ben-allred.audiophile.common.core.serdes.impl :as serde]
    [com.ben-allred.audiophile.common.core.serdes.protocols :as pserdes]
    [com.ben-allred.audiophile.common.core.utils.maps :as maps]))

#?(:clj
   (deftest edn-test
     (let [serde (serde/edn {})]
       (testing "serializes values to strings"
         (is (string? (serdes/serialize serde {:a 1 [:b :c] #{:d ()}}))))

       (testing "equal after a round trip through serialization"
         (is (= {:a 1 [:b :c] #{:d ()}}
                (->> {:a 1 [:b :c] #{:d ()}}
                     (serdes/serialize serde)
                     (serdes/deserialize serde)))))

       (testing "has a mime-type"
         (is (= "application/edn"
                (serdes/mime-type serde)))))))

(deftest transit-test
  (let [serde (serde/transit {})]
    (testing "serializes values to strings"
      (is (string? (serdes/serialize serde {:a 1 [:b :c] #{:d ()}}))))

    (testing "equal after a round trip through serialization"
      (is (= {:a 1 [:b :c] #{:d ()}}
             (->> {:a 1 [:b :c] #{:d ()}}
                  (serdes/serialize serde)
                  (serdes/deserialize serde)))))

    (testing "has a mime-type"
      (is (= "application/transit+json"
             (serdes/mime-type serde))))))

(deftest json-test
  (let [serde (serde/json {})]
    (testing "serializes values to strings"
      (is (string? (serdes/serialize serde {:a 1 [:b :c] #{:d ()}}))))

    (testing "equal after a round trip through serialization"
      (is (= {:a 1 :b ["c" ()]}
             (->> {:a 1 :b ["c" ()]}
                  (serdes/serialize serde)
                  (serdes/deserialize serde)))))

    (testing "has a mime-type"
      (is (= "application/json"
             (serdes/mime-type serde))))))

(deftest urlencode-test
  (let [serde (serde/urlencode {})]
    (testing "serializes values to strings"
      (is (string? (serdes/serialize serde {:a 1 :b true :c false :d nil :e [1 2 3]}))))

    (testing "equal after a round trip through serialization"
      (is (= {:a "1" :b true :e ["1" "2" "3"]}
             (->> {:a 1 :b true :c false :d nil :e [1 2 3]}
                  (serdes/serialize serde)
                  (serdes/deserialize serde)))))

    (testing "has a mime-type"
      (is (= "application/x-www-form-urlencoded"
             (serdes/mime-type serde))))))

#?(:clj
   (deftest jwt-test
     (let [serde (serde/jwt {:data-serde (serde/transit {})
                             :expiration 30
                             :secret     "secret"})]
       (testing "serializes values to strings"
         (is (string? (serdes/serialize serde {:a 1 :b true :c false :d nil :e [1 2 3]}))))

       (testing "equal after a round trip through serialization"
         (is (= {:a 1 [:b :c] #{:d ()}}
                (-> {:a 1 [:b :c] #{:d ()}}
                    (->> (serdes/serialize serde)
                         (serdes/deserialize serde))
                    (dissoc :jwt/exp :jwt/iat))))))))

(deftest find-serde-test
  (let [serde-1 (reify pserdes/IMime
                  (mime-type [_]
                    "serde/one"))
        serde-2 (reify pserdes/IMime
                  (mime-type [_]
                    "serde/two"))
        serde-3 (reify pserdes/IMime
                  (mime-type [_]
                    "serde/three"))
        serdes (maps/->m serde-1 serde-2)]
    (testing "finds serde by mime-type"
      (are [type expected] (= expected (serdes/find-serde serdes type serde-3))
        "serde/one" serde-1
        "serde/two" serde-2
        "serde/three" serde-3
        "serde/unknown" serde-3))))
