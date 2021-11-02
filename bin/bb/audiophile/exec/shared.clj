(ns audiophile.exec.shared
  (:require
    [babashka.process :as p]
    [clojure.string :as string]))

(defn ^:private dispatch-fn [k _]
  (keyword k))

(defmulti build* dispatch-fn)

(defmulti clean* dispatch-fn)

(defmulti main* dispatch-fn)

(defmulti rabbit* dispatch-fn)

(defmulti run* dispatch-fn)

(defmulti test* dispatch-fn)

(defmulti wipe* dispatch-fn)

(defn with-default-env [m]
  (into {}
        (map (fn [[k v]]
               [k (or (System/getenv k) v)]))
        m))

(defn process!
  ([command]
   (process! command {}))
  ([command env]
   (let [status (:exit @(p/process ["sh" "-c" command]
                                   {:inherit   true
                                    :extra-env env}))]
     (when-not (zero? status)
       (throw (ex-info "non-zero status" {:command command :env env}))))))

(defn clj
  ([arg]
   (clj nil arg))
  ([aliases arg]
   (let [aliases (some->> aliases seq string/join (str " -A"))
         args (cond->> arg (not (string? arg)) (string/join " "))]
     (str "clj" aliases " -Sthreads 1 " args))))
