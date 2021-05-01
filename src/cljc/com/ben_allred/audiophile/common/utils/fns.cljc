(ns com.ben-allred.audiophile.common.utils.fns
  #?(:cljs
     (:require-macros
       [com.ben-allred.audiophile.common.utils.fns])))

(defn sidecar!
  "takes a sequence of fns and returns a function that calls all of them with the same args, but only returns
   the value of the first fn. Useful for side effectful things."
  ([f] f)
  ([f & fns]
   (comp first (apply juxt (cons f fns)))))

(defn flip
  "returns a function that applies f with the first two args swapped."
  [f]
  (fn [a b & args]
    (apply f b a args)))

(defmacro => [& forms]
  `(fn [arg#]
     (-> arg# ~@forms)))

(defmacro =>> [& forms]
  `(fn [arg#]
     (->> arg# ~@forms)))