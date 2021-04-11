(ns com.ben-allred.audiophile.api.services.serdes.core
  (:require
    [clojure.edn :as edn]
    [com.ben-allred.audiophile.api.services.serdes.protocol :as pserdes]))

(deftype EdnSerde []
  pserdes/ISerde
  (serialize [_ value _]
    (pr-str value))
  (deserialize [_ value opts]
    (if (string? value)
      (edn/read-string opts value)
      (edn/read opts value))))

(def edn
  (->EdnSerde))

(defn serialize
  ([serde value]
   (serialize serde value nil))
  ([serde value opts]
   (pserdes/serialize serde value opts)))

(defn deserialize
  ([serde value]
   (deserialize serde value nil))
  ([serde value opts]
   (pserdes/deserialize serde value opts)))
