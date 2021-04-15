(ns com.ben-allred.audiophile.common.services.serdes.core
  (:require
    #?@(:clj [[clj-jwt.core :as clj-jwt]
              [clojure.java.io :as io]])
    [#?(:clj clojure.edn :cljs cljs.reader) :as edn]
    [cognitect.transit :as trans]
    [com.ben-allred.audiophile.common.services.serdes.protocols :as pserdes]
    [integrant.core :as ig])
  #?(:clj
     (:import
       (java.io ByteArrayInputStream ByteArrayOutputStream InputStream PushbackReader)
       (java.time.temporal ChronoUnit)
       (java.util Date)
       (org.joda.time DateTime))))

(defmethod ig/init-key ::edn [_ _]
  (reify
    pserdes/ISerde
    (mime-type [_]
      "application/edn")
    (serialize [_ value _]
      (pr-str value))
    (deserialize [_ value opts]
      (try
        (cond
          (nil? value) nil
          (string? value) (edn/read-string opts value)
          #?@(:clj [(instance? InputStream value) (some->> value io/reader PushbackReader. (edn/read opts))])
          :else (edn/read opts value))
        (catch #?(:cljs :default :default Throwable) _ nil)))))

(defmethod ig/init-key ::transit [_ _]
  (reify
    pserdes/ISerde
    (mime-type [_]
      "application/json+transit")
    (serialize [_ value _]
      #?(:clj (let [out (ByteArrayOutputStream. 4096)]
                (-> out
                    (trans/writer :json)
                    (trans/write value))
                (.toString out))
         :cljs (-> :json
                   trans/writer
                   (trans/write value))))
    (deserialize [_ value _]
      (try
        #?(:clj  (-> value
                     (cond->
                       (not (instance? InputStream value))
                       (-> .getBytes ByteArrayInputStream.))
                     (trans/reader :json)
                     trans/read)
           :cljs (-> :json
                     trans/reader
                     (trans/read value)))
        (catch #?(:cljs :default :default Throwable) _ nil)))))

(defmethod ig/init-key ::jwt [_ {:keys [algo data-serde expiration secret]}]
  (reify
    pserdes/ISerde
    (serialize [_ payload opts]
      #?(:clj
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
                clj-jwt/to-str))))
    (deserialize [_ token opts]
      #?(:clj
          (try
            (some-> (let [jwt (clj-jwt/str->jwt token)]
                      (when (clj-jwt/verify jwt algo secret)
                        jwt))
                    :claims
                    (update :data (partial pserdes/deserialize data-serde) opts))
            (catch Throwable _ nil))))))

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
