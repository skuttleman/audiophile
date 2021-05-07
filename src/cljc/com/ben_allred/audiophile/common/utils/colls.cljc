(ns com.ben-allred.audiophile.common.utils.colls
  (:require
    [clojure.walk :as walk]
    [com.ben-allred.audiophile.common.utils.strings :as strings]))

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

(defn only!
  ([coll]
   (first (only! 1 coll)))
  ([n coll]
   (when (seq (drop n coll))
     (throw (ex-info (strings/format "expected at most %d item(s), but got more" n)
                     {:coll coll})))
   (take n coll)))

(defn split-on [pred coll]
  [(filter pred coll) (remove pred coll)])
