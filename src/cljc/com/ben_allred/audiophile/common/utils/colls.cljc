(ns com.ben-allred.audiophile.common.utils.colls
  (:require
    [clojure.walk :as walk]))

(defn cons?
  "is collection sequential and not a vector (list, lazy seq, etc)"
  [coll]
  (and (sequential? coll)
       (not (vector? coll))))

(defn postwalk
  "recursively walk a data structure applying f to every value"
  [f coll]
  (walk/postwalk f coll))
