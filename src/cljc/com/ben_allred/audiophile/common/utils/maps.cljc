(ns com.ben-allred.audiophile.common.utils.maps
  (:refer-clojure :exclude [flatten]))

(defn update-maybe [m k f & f-args]
  (if (some? (get m k))
    (apply update m k f f-args)
    m))

(defn update-in-maybe [m [k :as ks] f & f-args]
  (cond
    (and (empty? ks) (some? m)) (apply f m f-args)
    (and (seq ks) (some? (get m k))) (update m k (partial apply update-in-maybe) (rest ks) f f-args)
    :else m))

(defn assoc-maybe [m & kvs]
  (into (or m {}) (comp (partition-all 2) (filter (comp some? second))) kvs))

(defn assoc-in-maybe [m ks v]
  (cond-> m
    (some? v) (assoc-in ks v)))

(defn dissocp [m pred]
  (when m
    (let [m' (meta m)]
      (cond-> (into {} (remove (comp pred second)) m)
        m' (with-meta m')))))

(defn walk [m f]
  (when m
    (-> {}
        (into (map (fn [[k v]]
                     (let [v' (if (map? v) (walk v f) v)]
                       (f k v'))))
              m)
        (with-meta (meta m)))))

(defn ^:private flatten* [m path]
  (mapcat (fn [[k v]]
            (let [path' (conj path k)]
              (if (map? v)
                (flatten* v path')
                [[path' v]])))
          m))

(defn flatten [m]
  (into {} (flatten* m [])))

(defn nest [m]
  (reduce-kv assoc-in {} m))

(defn deep-merge [m & ms]
  (if (map? m)
    (apply merge-with deep-merge m ms)
    (last ms)))
