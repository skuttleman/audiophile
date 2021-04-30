(ns com.ben-allred.audiophile.common.utils.maps
  #?(:cljs (:require-macros com.ben-allred.audiophile.common.utils.maps))
  (:refer-clojure :exclude [flatten])
  (:require
    [medley.core :as medley]))

(defn update-maybe
  "updates k on a map - only if that value is not nil"
  [m k f & f-args]
  (if (some? (get m k))
    (apply update m k f f-args)
    m))

(defn update-in-maybe
  "updates a nested value in a map - only if that value is not nil"
  [m [k :as ks] f & f-args]
  (cond
    (and (empty? ks) (some? m)) (apply f m f-args)
    (and (seq ks) (some? (get m k))) (update m k (partial apply update-in-maybe) (rest ks) f f-args)
    :else m))

(defn assoc-defaults [m & kvs]
  "assoc values onto a map when the value of k is nil"
  {:pre [(assert (even? (count kvs)) "must provide an even number of forms to assoc-defaults")]}
  (into (or m {})
        (comp (partition-all 2)
              (remove (comp some? (partial get m) first)))
        kvs))

(defn assoc-maybe [m & kvs]
  "assoc values onto a map when the values provided are not nil"
  {:pre [(assert (even? (count kvs)) "must provide an even number of forms to assoc-maybe")]}
  (into (or m {})
        (comp (partition-all 2)
              (filter (comp some? second)))
        kvs))

(defn assoc-in-maybe [m ks v]
  "assoc v into m when v is not nil"
  (cond-> m
    (some? v) (assoc-in ks v)))

(defn dissocp [m pred]
  "remove all keys from m that satisfy pred"
  (when m
    (let [m' (meta m)]
      (cond-> (into {} (remove (comp pred second)) m)
        m' (with-meta m')))))

(defn ^:private flatten* [m path]
  (mapcat (fn [[k v]]
            (let [path' (conj path k)]
              (if (map? v)
                (flatten* v path')
                [[path' v]])))
          m))

(defn flatten [m]
  "give a map m with potentially nested maps, flatten into a top level map where all the
   keys are vectors representing the path into the original map"
  (into {} (flatten* m [])))

(defn nest [m]
  "given a map where all of its keys represent a path into a nested data structure,
   generated the representational data structure"
  (reduce-kv assoc-in {} m))

(defn deep-merge [m & ms]
  "like merge, but handles nested collisions as well using stack-based recursion"
  (if (map? m)
    (apply merge-with deep-merge m ms)
    (last ms)))

(def ^{:arglists '([f coll])} map-keys
  "creates a new map where all the keys are the result of calling f"
  medley/map-keys)

(def ^{:arglists '([f coll] [f c1 & colls])} map-vals
  "creates a new map where all the values are the result of calling f"
  medley/map-vals)

(defmacro ->m
  "Compiles a sequence of symbols into a map literal of (keyword symbol) -> symbol.
   Also allows vectors of key/value pairs. Optionally handles a map literal as first arg.
  (->m foo bar [baz :also]) => {:foo foo :bar bar baz :also}"
  [m? & kvs]
  (let [[m kvs] (if (map? m?)
                  [m? kvs]
                  [{} (cons m? kvs)])]
    (loop [m (transient m) [x :as kvs] kvs]
      (cond
        (empty? kvs) (persistent! m)
        (symbol? x) (recur (assoc! m (keyword x) x) (next kvs))
        (or (map-entry? x) (vector? x)) (recur (conj! m x) (next kvs))
        :else (throw (ex-info "must pass symbols, map entries, or vector tuples" {:bad-value x}))))))

(defn extract-keys
  "Takes a map and a sequence of keys and returns a vector with a map
   with only the specified keys and another map with all but those keys"
  [m keys]
  [(select-keys m keys) (apply dissoc m keys)])
