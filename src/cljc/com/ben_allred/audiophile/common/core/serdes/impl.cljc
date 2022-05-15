(ns com.ben-allred.audiophile.common.core.serdes.impl
  (:require
    #?@(:clj [[buddy.sign.jwt :as jwt*]
              [clojure.java.io :as io]
              [jsonista.core :as jsonista]]
        :cljs [[com.cognitect.transit.types :as ty]])
    [#?(:clj clojure.edn :cljs cljs.reader) :as edn*]
    [cognitect.transit :as trans]
    [com.ben-allred.audiophile.common.core.serdes.protocols :as pserdes]
    [com.ben-allred.audiophile.common.core.utils.core :as u]
    [com.ben-allred.audiophile.common.core.utils.keywords :as keywords]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.common.core.utils.maps :as maps]
    [com.ben-allred.audiophile.common.core.utils.uri :as uri])
  #?(:clj
     (:import
       (java.io ByteArrayInputStream ByteArrayOutputStream InputStream PushbackReader)
       (java.time.temporal ChronoUnit)
       (java.util Date Base64 Base64$Decoder Base64$Encoder)
       (org.joda.time DateTime)
       (java.net URL))))

#?(:cljs
   (extend-type ty/UUID IUUID))

(def ^:private object-mapper
  #?(:clj     (jsonista/object-mapper
                {:encode-key-fn keywords/str
                 :decode-key-fn keyword})
     :default nil))

(def ^:private base64-mappers
  {#?@(:clj [:encoder (Base64/getUrlEncoder)
             :decoder (Base64/getUrlDecoder)])})

(defn ^:private edn#serialize [value]
  (pr-str value))

(defn ^:private edn#deserialize
  ([value]
   (edn#deserialize value nil))
  ([value opts]
   (let [opts (or opts {})]
     (u/silent!
       (cond
         (nil? value) nil
         (string? value) (edn*/read-string opts value)
         #?@(:clj [(or (instance? InputStream value)
                       (instance? URL value))
                   (some->> value io/reader PushbackReader. (edn*/read opts))])
         :else (edn*/read opts value))))))

(def edn
  (reify
    pserdes/ISerde
    (serialize [_ value _]
      (edn#serialize value))
    (deserialize [_ value opts]
      (edn#deserialize value opts))

    pserdes/IMime
    (mime-type [_]
      "application/edn")))

(defn ^:private transit#serialize [value]
  #?(:clj  (let [out (ByteArrayOutputStream. 4096)]
             (-> out
                 (trans/writer :json)
                 (trans/write value))
             (.toString out))
     :cljs (-> :json
               trans/writer
               (trans/write value))))

(defn ^:private transit#deserialize [value]
  (u/silent!
    #?(:clj  (-> value
                 (cond->
                   (not (instance? InputStream value))
                   (-> .getBytes ByteArrayInputStream.))
                 (trans/reader :json)
                 trans/read)
       :cljs (-> :json
                 trans/reader
                 (trans/read value)))))

(def transit
  (reify
    pserdes/ISerde
    (serialize [_ value _]
      (transit#serialize value))
    (deserialize [_ value _]
      (transit#deserialize value))

    pserdes/IMime
    (mime-type [_]
      "application/transit+json")))

(defn ^:private json#serialize [value]
  #?(:clj  (jsonista/write-value-as-string value object-mapper)
     :cljs (js/JSON.stringify (clj->js value :keyword-fn keywords/str))))

(defn ^:private json#deserialize [value]
  #?(:clj (jsonista/read-value value object-mapper)
     :cljs (js->clj (js/JSON.parse value) :keywordize-keys true)))

(def json
  (reify
    pserdes/ISerde
    (serialize [_ value _]
      (json#serialize value))
    (deserialize [_ value _]
      (json#deserialize value))

    pserdes/IMime
    (mime-type [_]
      "application/json")))

(def urlencode
  (reify
    pserdes/ISerde
    (serialize [_ value _]
      (uri/join-query value))
    (deserialize [_ value _]
      (uri/split-query value))

    pserdes/IMime
    (mime-type [_]
      "application/x-www-form-urlencoded")))

(def base64
  (reify
    pserdes/ISerde
    (serialize [_ payload _]
      #?(:clj (->> payload
                   ^String edn#serialize
                   .getBytes
                   (.encodeToString ^Base64$Encoder (:encoder base64-mappers)))))
    (deserialize [_ payload _]
      #?(:clj (->> ^String payload
                   (.decode ^Base64$Decoder (:decoder base64-mappers))
                   String.
                   edn#deserialize)))))

#?(:clj
   (defn ^:private date->date-time [^Date date]
     (-> date .getTime DateTime.)))

(defn jwt [{:keys [expiration secret]}]
  (reify
    pserdes/ISerde
    (serialize [_ payload opts]
      #?(:clj
          (let [now (Date.)
                expiration (:jwt/expiration opts expiration)
                unit (case (:jwt/unit opts)
                       :minutes ChronoUnit/MINUTES
                       :hours ChronoUnit/HOURS
                       ChronoUnit/DAYS)]
            (-> (:jwt/claims opts)
                (assoc :iat (date->date-time now)
                       :data (pserdes/serialize transit payload opts)
                       :exp (-> now
                                .toInstant
                                (.plus expiration unit)
                                Date/from
                                date->date-time))
                (jwt*/sign secret)))))
    (deserialize [_ token opts]
      #?(:clj
          (u/silent!
            (when-let [value (some-> token (jwt*/unsign secret))]
              (-> value
                  (dissoc :data)
                  (maps/update-maybe :aud (partial into #{} (map keyword)))
                  (maps/qualify :jwt)
                  (merge (pserdes/deserialize transit (:data value) opts)))))))))

(def serdes
  (maps/->m [:default edn] edn json transit urlencode))
