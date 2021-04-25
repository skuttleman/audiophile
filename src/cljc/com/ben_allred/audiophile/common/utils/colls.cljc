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

(defn force-sequential [item]
  (cond-> item
    (not (sequential? item)) vector))

(defn only! [coll]
  (let [[item & more] coll]
    (when (seq more)
      (throw (ex-info "expected singleton collection, but got more than one item" {})))
    item))
