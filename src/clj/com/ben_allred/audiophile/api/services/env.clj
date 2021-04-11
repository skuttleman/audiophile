(ns com.ben-allred.audiophile.api.services.env
  (:refer-clojure :exclude [get])
  (:require
    [camel-snake-kebab.core :as csk]
    [clojure.string :as string]
    [clojure.java.io :as io]
    [com.ben-allred.audiophile.api.services.serdes.core :as serdes]))

(def ^:private env-spec
  '[[:server-port {:parser  :long
                   :default 3000}]
    [:nrepl-port {:parser  :long
                  :default 7000}]])

(defn ^:private ->value [spec value parser]
  (if (some? value)
    (parser value)
    (:default spec)))

(defmulti parse-env-var (fn [spec _] (:parser spec)))

(defmethod parse-env-var :default
  [spec value]
  (->value spec value identity))

(defmethod parse-env-var :long
  [spec value]
  (->value spec value #(Long/parseLong %)))

(defmethod parse-env-var :bool
  [spec value]
  (->value spec value (comp boolean #{"true" "yes" "t" "y"} string/lower-case)))

(defmethod parse-env-var :edn
  [spec value]
  (->value spec value (partial serdes/deserialize serdes/edn)))

(defn file->env [file]
  (some->> (io/resource file)
           slurp
           (serdes/deserialize serdes/edn)))

(def ^:private static-env
  (merge {}
         (System/getenv)
         (file->env ".env")))

(defn ^:private build-env [m]
  (into {}
        (map (fn [[k spec]]
               [k (parse-env-var spec
                                 (clojure.core/get m
                                                   (csk/->SCREAMING_SNAKE_CASE_STRING k)))]))
        env-spec))

(def get (build-env static-env))

(defn load-env! [env file]
  (alter-var-root #'get (constantly (build-env (merge static-env
                                                      (file->env file)
                                                      env)))))
