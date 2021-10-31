(ns com.ben-allred.audiophile.common.core.utils.colls
  (:require
    [clojure.walk :as walk]
    [com.ben-allred.audiophile.common.core.utils.strings :as strings]))

(defn cons?
  "Tests if collection is sequential and not a vector (list, cons, lazy seq, etc)"
  [coll]
  (and (sequential? coll)
       (not (vector? coll))))

(defn postwalk
  "Recursively walk a data structure applying f to every element"
  [f coll]
  (walk/postwalk f coll))

(defn force-sequential
  "Wraps `item` in a sequential singleton collection if it's not already a sequential collection"
  [item]
  (cond-> item
    (not (sequential? item)) vector))

(defn force-seq
  "Wraps `item` in a sequential singleton collection if it's not already seqable (other than string"
  [item]
  (cond-> item
    (and (not (string? item))
         (not (seqable? item)))
    vector))

(defn only!
  "arity-1 - if `coll` has more than one item, throws an exception.
             otherwise it returns the first item
   arity-2 - if `coll` has more than `n` item(s), throws an exception.
             otherwise it returns the first `n` items"
  ([coll]
   (first (only! 1 coll)))
  ([n coll]
   (when (seq (drop n coll))
     (throw (ex-info (strings/format "expected at most %d item(s), but got more" n)
                     {:coll coll})))
   (take n coll)))

(defn split-on
  "Splits `coll` into a vector of two seqs. The first seq contains all items of `coll`
   that satisfy the predicate. The second seq contains all items of `coll` that do not
   satisfy the predicate.

   ```clojure
   (split-on even? [1 2 3 4 5 6 7])
   ;; => [(2 4 6) (1 3 5 7)]
   ```"
  [pred coll]
  [(filter pred coll) (remove pred coll)])


(defn nest-children
  "Recursively nests a collection of maps under the `nest-k` key where
   `(= (get m1 parent-k) (get m2 id-k))`"
  [id-k parent-k nest-k coll]
  (let [m (group-by #(get % parent-k) coll)]
    (letfn [(nest* [[item :as items] result]
              (let [children (get m (get item id-k))]
                (if (empty? items)
                  result
                  (recur (rest items)
                         (conj result (cond-> item
                                        (seq children)
                                        (assoc nest-k (nest* children []))))))))]
      (nest* (get m nil) []))))
