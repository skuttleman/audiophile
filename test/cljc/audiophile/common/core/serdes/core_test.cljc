(ns ^:unit audiophile.common.core.serdes.core-test
  (:require
    [clojure.test :refer [are deftest is testing]]
    [audiophile.common.core.serdes.core :as serdes]
    [audiophile.common.core.serdes.impl :as serde]
    [audiophile.common.core.serdes.protocols :as pserdes]
    [audiophile.common.core.utils.maps :as maps]))

(deftest edn-test
  (testing "serializes values to strings"
    (is (string? (serdes/serialize serde/edn {:a 1 [:b :c] #{:d ()}}))))

  (testing "equal after a round trip through serialization"
    (is (= {:a 1 [:b :c] #{:d ()}}
           (->> {:a 1 [:b :c] #{:d ()}}
                (serdes/serialize serde/edn)
                (serdes/deserialize serde/edn)))))

  (testing "has a mime-type"
    (is (= "application/edn"
           (serdes/mime-type serde/edn)))))

(deftest transit-test
  (testing "serializes values to strings"
    (is (string? (serdes/serialize serde/transit {:a 1 [:b :c] #{:d ()}}))))

  (testing "equal after a round trip through serialization"
    (is (= {:a 1 [:b :c] #{:d ()}}
           (->> {:a 1 [:b :c] #{:d ()}}
                (serdes/serialize serde/transit)
                (serdes/deserialize serde/transit)))))

  (testing "has a mime-type"
    (is (= "application/transit+json"
           (serdes/mime-type serde/transit)))))

(deftest json-test
  (testing "serializes values to strings"
    (is (string? (serdes/serialize serde/json {:a 1 [:b :c] #{:d ()}}))))

  (testing "equal after a round trip through serialization"
    (is (= {:a 1 :b ["c" ()]}
           (->> {:a 1 :b ["c" ()]}
                (serdes/serialize serde/json)
                (serdes/deserialize serde/json)))))

  (testing "has a mime-type"
    (is (= "application/json"
           (serdes/mime-type serde/json)))))

(deftest urlencode-test
  (testing "serializes values to strings"
    (is (string? (serdes/serialize serde/urlencode {:a 1 :b true :c false :d nil :e [1 2 3]}))))

  (testing "equal after a round trip through serialization"
    (is (= {:a "1" :b true :e ["1" "2" "3"]}
           (->> {:a 1 :b true :c false :d nil :e [1 2 3]}
                (serdes/serialize serde/urlencode)
                (serdes/deserialize serde/urlencode)))))

  (testing "has a mime-type"
    (is (= "application/x-www-form-urlencoded"
           (serdes/mime-type serde/urlencode)))))

#?(:clj
   (deftest jwt-test
     (let [serde (serde/jwt {:expiration 30
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
