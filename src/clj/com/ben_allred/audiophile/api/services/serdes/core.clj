(ns com.ben-allred.audiophile.api.services.serdes.core
  (:require
    [clj-jwt.core :as clj-jwt]
    [clojure.edn :as edn]
    [clojure.java.io :as io]
    [cognitect.transit :as trans]
    [com.ben-allred.audiophile.api.services.serdes.protocol :as pserdes]
    [com.ben-allred.audiophile.common.utils.macros :as macros]
    [integrant.core :as ig])
  (:import
    (java.io ByteArrayInputStream ByteArrayOutputStream InputStream PushbackReader)
    (java.time.temporal ChronoUnit)
    (java.util Date)
    (org.joda.time DateTime)))

(defmethod ig/init-key ::edn [_ _]
  (reify
    pserdes/ISerde
    (mime-type [_]
      "application/edn")
    (serialize [_ value _]
      (pr-str value))
    (deserialize [_ value opts]
      (macros/ignore!
        (cond
          (nil? value) nil
          (string? value) (edn/read-string opts value)
          (instance? InputStream value) (some->> value io/reader PushbackReader. (edn/read opts))
          :else (edn/read opts value))))))

(defmethod ig/init-key ::transit [_ _]
  (reify
    pserdes/ISerde
    (mime-type [_]
      "application/json+transit")
    (serialize [_ value _]
      (let [out (ByteArrayOutputStream. 4096)]
        (-> out
            (trans/writer :json)
            (trans/write value))
        (.toString out)))
    (deserialize [_ value _]
      (macros/ignore!
        (-> value
            (cond->
              (not (instance? InputStream value))
              (-> .getBytes ByteArrayInputStream.))
            (trans/reader :json)
            trans/read)))))

(defmethod ig/init-key ::jwt [_ {:keys [algo data-serde expiration secret]}]
  (reify
    pserdes/ISerde
    (serialize [_ payload opts]
      (let [now (Date.)]
        (-> {:iat  (-> now .getTime DateTime.)
             :data (pserdes/serialize data-serde payload opts)
             :exp  (-> now
                       .toInstant
                       (.plus expiration ChronoUnit/DAYS)
                       Date/from
                       .getTime
                       DateTime.)}
            clj-jwt/jwt
            (clj-jwt/sign algo secret)
            clj-jwt/to-str)))
    (deserialize [_ token opts]
      (macros/ignore!
        (some-> (let [jwt (clj-jwt/str->jwt token)]
                  (when (clj-jwt/verify jwt algo secret)
                    jwt))
                :claims
                (update :data (partial pserdes/deserialize data-serde) opts))))))

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