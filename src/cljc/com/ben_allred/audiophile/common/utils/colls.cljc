(ns com.ben-allred.audiophile.common.utils.colls)

(defn cons? [coll]
  (and (sequential? coll)
       (not (vector? coll))))
