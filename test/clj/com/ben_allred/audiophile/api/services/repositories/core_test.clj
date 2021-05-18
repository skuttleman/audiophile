(ns ^:unit com.ben-allred.audiophile.api.services.repositories.core-test
  (:require
    [clojure.set :as set]
    [clojure.test :refer [are deftest is testing]]
    [com.ben-allred.audiophile.api.services.repositories.core :as repos]
    [com.ben-allred.audiophile.common.utils.uuids :as uuids]
    [integrant.core :as ig]
    [next.jdbc.result-set :as result-set])
  (:import
    (java.sql ResultSet ResultSetMetaData)))

(deftype StubMeta [key]
  ResultSetMetaData
  (getColumnCount [_]
    1)
  (getColumnLabel [_ _]
    key))

(deftype StubResultSet [key val]
  ResultSet
  (getMetaData [_]
    (->StubMeta key))
  (getObject [_ ^int _]
    (reify
      result-set/ReadableColumn
      (read-column-by-index [_ _ _]
        val))))

(deftest ->builder-fn-test
  (testing "Builder"
    (let [->builder-fn (ig/init-key ::repos/->builder-fn nil)
          id (uuids/random)
          result-set (->StubResultSet "user/id" id)]
      (testing "#with-column"
        (let [builder-fn (->builder-fn {:model-fn (fn [[k v]] [v k])})
              row (transient {})
              builder (builder-fn result-set nil)
              result (persistent! (result-set/with-column builder row 1))]
          (is (= {id :user/id} result))))

      (testing "#with-row"
        (let [builder-fn (->builder-fn {:result-xform (fn [rf]
                                                        (fn [result item]
                                                          (rf (rf result item) (set/map-invert item))))})
              builder (builder-fn result-set nil)
              cols (transient [])
              result (persistent! (result-set/with-row builder cols {:foo :bar}))]
          (is (= [{:foo :bar} {:bar :foo}] result)))))))
