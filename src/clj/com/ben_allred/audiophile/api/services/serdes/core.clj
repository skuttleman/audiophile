(ns com.ben-allred.audiophile.api.services.serdes.core
  (:require
    [clojure.edn :as edn]
    [clojure.java.io :as io]
    [com.ben-allred.audiophile.api.services.serdes.protocol :as pserdes]
    [com.ben-allred.audiophile.common.utils.logger :as log])
  (:import
    (java.io InputStream PushbackReader)))

(def edn
  (reify
    pserdes/ISerde
    (mime-type [_]
      "application/edn")
    (serialize [_ value _]
      (pr-str value))
    (deserialize [_ value opts]
      (cond
        (nil? value) nil
        (string? value) (edn/read-string opts value)
        (instance? InputStream value) (try (some->> value io/reader PushbackReader. (edn/read opts))
                                           (catch Throwable _
                                             nil))
        :else (edn/read opts value)))))

(defn serialize
  ([serde value]
   (serialize serde value {}))
  ([serde value opts]
   (pserdes/serialize serde value opts)))

(defn deserialize
  ([serde value]
   (deserialize serde value {}))
  ([serde value opts]
   (pserdes/deserialize serde value opts)))

(defn mime-type [serde]
  (pserdes/mime-type serde))
