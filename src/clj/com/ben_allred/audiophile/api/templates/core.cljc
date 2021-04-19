(ns com.ben-allred.audiophile.api.templates.core
  (:require
    [clojure.string :as string]
    [com.ben-allred.audiophile.api.templates.html :as html]
    [com.ben-allred.audiophile.common.utils.colls :as colls]
    [com.ben-allred.audiophile.common.utils.maps :as maps])
  (:import
    (java.util Map$Entry)))

(declare ^:private render)

(defn ^:private m->css [m]
  (if (map? m)
    (->> m
         (map (fn [[k v]] (str (name k) ": " v)))
         (string/join ";"))
    m))

(defn ^:private coll->class [class]
  (if (string? class)
    class
    (string/join " " (filter some? class))))

(defn ^:private clean-attrs [attrs]
  (-> attrs
      (->> (colls/postwalk (fn [x]
                            (if (instance? Map$Entry x)
                              (let [v (val x)]
                                (when-not (or (nil? v) (fn? v))
                                  [(key x) (cond-> v
                                             (keyword? v) name)]))
                              x))))
      (maps/update-maybe :class coll->class)
      (maps/update-maybe :style m->css)))

(defn ^:private render* [arg]
  (cond
    (vector? arg) (if (= :<> (first arg))
                    (map render (rest arg))
                    (render arg))
    (colls/cons? arg) (map render arg)
    (map? arg) (clean-attrs arg)
    :else arg))

(defn ^:private render [[node & args :as tree]]
  (when tree
    (let [[node & args] (if (fn? node)
                          (loop [node (apply node args)]
                            (if (fn? node)
                              (recur (apply node args))
                              (render node)))
                          tree)]
      (into [node] (map render*) args))))

(defn html [tree]
  (html/render (render tree)))
