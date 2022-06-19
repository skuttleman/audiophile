(ns spigot.context
  (:refer-clojure :exclude [get resolve])
  (:require
    [clojure.walk :as walk]
    [spigot.protocols :as psp])
  #?(:clj
     (:import (clojure.lang TaggedLiteral))))

(defmulti ^:private resolve-tagged
          (fn [tag _ _]
            tag))

(extend-protocol psp/ICtxResolver
  TaggedLiteral
  (resolve [this ctx]
    (resolve-tagged (:tag this) (:form this) ctx)))

(defn get [k]
  (tagged-literal 'spigot.ctx/get k))

(def readers
  {'spigot.ctx/get get})

(defn resolve-params [params ctx]
  (walk/prewalk (fn [x]
                  (cond-> x
                    (satisfies? psp/ICtxResolver x) (psp/resolve ctx)))
                params))

(defn merge-ctx [ctx ->ctx result]
  (reduce (fn [ctx [param k]]
            (if-let [[_ v] (find result k)]
              (assoc ctx param v)
              ctx))
          ctx
          ->ctx))

(defmethod resolve-tagged 'spigot.ctx/get
  [_ key ctx]
  (clojure.core/get ctx key))
