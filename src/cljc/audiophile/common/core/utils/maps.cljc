(ns audiophile.common.core.utils.maps
  #?(:cljs
     (:require-macros
       audiophile.common.core.utils.maps))
  (:refer-clojure :exclude [flatten])
  (:require
    [medley.core :as medley]))

(defn update-maybe
  "Updates k on a map - only if that value is not nil"
  [m k f & f-args]
  (if (some? (get m k))
    (apply update m k f f-args)
    m))

(defn update-in-maybe
  "Updates a nested value in a map - only if that value is not nil"
  [m [k :as ks] f & f-args]
  (cond
    (and (empty? ks) (some? m)) (apply f m f-args)
    (and (seq ks) (some? (get m k))) (update m k (partial apply update-in-maybe) (rest ks) f f-args)
    :else m))

(defn assoc-defaults [m & kvs]
  "Assoc values onto a map when the current value of k is nil"
  {:pre [(assert (even? (count kvs)) "must provide an even number of forms to assoc-defaults")]}
  (into (or m {})
        (comp (partition-all 2)
              (remove (comp some? (partial get m) first)))
        kvs))

(defn assoc-maybe [m & kvs]
  "Assoc values onto a map when the values provided are not nil"
  {:pre [(assert (even? (count kvs)) "must provide an even number of forms to assoc-maybe")]}
  (into (or m {})
        (comp (partition-all 2)
              (filter (comp some? second)))
        kvs))

(defn assoc-in-maybe [m ks v]
  "Assoc v into m when v is not nil"
  (cond-> m
    (some? v) (assoc-in ks v)))

(defn dissocp [m pred]
  "Remove all keys from m that satisfy pred"
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
  "Given a map m with potentially nested maps, flatten into a top level map where all the
   keys are vectors representing the path into the original map"
  (into {} (flatten* m [])))

(defn nest [m]
  "Given a map where all of its keys represent a path into a nested data structure,
   generated the representational data structure"
  (reduce-kv assoc-in {} m))

(defn deep-merge [m & ms]
  "Like merge, but handles nested collisions as well using stack-based recursion"
  (cond
    (map? m) (apply merge-with deep-merge m ms)
    (empty? ms) m
    :else (last ms)))

(defn map-keys [f coll]
  "creates a new map where all the keys are the result of calling f"
  (into (with-meta {} (meta coll))
        (map (fn [[k v]]
                  [(f k) v]))
        coll))

(defmacro ->m
  "Compiles a sequence of symbols into a map literal of (keyword symbol) -> symbol.
   Optional tries to add any other value on to the returned map via conj.

   ```clojure
   (->m foo bar [baz :also]) => {:foo foo :bar bar baz :also}
   ```"
  [& kvs]
  (loop [m (transient {}) [x :as kvs] kvs]
    (cond
      (empty? kvs) (persistent! m)
      (symbol? x) (recur (assoc! m (keyword x) x) (next kvs))
      :else (recur (conj! m x) (next kvs)))))

(defn extract-keys
  "Takes a map and a sequence of keys and returns a vector with a map
   with only the specified keys and another map with all but those keys"
  [m keys]
  [(select-keys m keys) (apply dissoc m keys)])

(defn qualify
  "Qualifies all keys of a map with a ns, disregarding the key's current namespace.
   If there are keys with duplicate names, one entry will win non-deterministically."
  [m ns]
  (when m
    (medley/map-keys (comp (partial keyword (name ns)) name) m)))

(defn select
  "Selects all keys that match a predicate"
  [m pred]
  (when m
    (medley/filter-keys pred m)))
