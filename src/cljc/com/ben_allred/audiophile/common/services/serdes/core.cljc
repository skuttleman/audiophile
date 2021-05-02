(ns com.ben-allred.audiophile.common.services.serdes.core
  (:require
    #?@(:clj [[clj-jwt.core :as clj-jwt]
              [clojure.java.io :as io]
              [jsonista.core :as jsonista]])
    [#?(:clj clojure.edn :cljs cljs.reader) :as edn*]
    [clojure.string :as string]
    [cognitect.transit :as trans]
    [com.ben-allred.audiophile.common.services.serdes.protocols :as pserdes]
    [com.ben-allred.audiophile.common.utils.core :as u]
    [com.ben-allred.audiophile.common.utils.keywords :as keywords]
    [com.ben-allred.audiophile.common.utils.logger :as log]
    [com.ben-allred.audiophile.common.utils.uri :as uri]
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
      (u/silent!
        (cond
          (nil? value) nil
          (string? value) (edn*/read-string opts value)
          #?@(:clj [(instance? InputStream value) (some->> value io/reader PushbackReader. (edn*/read opts))])
          :else (edn*/read opts value))))))

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
      (u/silent!
        #?(:clj  (-> value
                     (cond->
                       (not (instance? InputStream value))
                       (-> .getBytes ByteArrayInputStream.))
                     (trans/reader :json)
                     trans/read)
           :cljs (-> :json
                     trans/reader
                     (trans/read value)))))))

(defmethod ig/init-key ::json [_ {:keys [object-mapper]}]
  (reify
    pserdes/ISerde
    (mime-type [_]
      "application/json")
    (serialize [_ value _]
      #?(:clj  (jsonista/write-value-as-string value object-mapper)
         :cljs (js/JSON.stringify value)))
    (deserialize [_ value _]
      #?(:clj (jsonista/read-value value object-mapper)
         :cljs (js/JSON.parse value)))))

(defmethod ig/init-key ::urlencode [_ _]
  (reify
    pserdes/ISerde
    (mime-type [_]
      "application/x-www-form-urlencoded")
    (serialize [_ value _]
      (uri/join-query value))
    (deserialize [_ value _]
      (uri/split-query value))))

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
          (u/silent!
            (some-> (let [jwt (clj-jwt/str->jwt token)]
                      (when (clj-jwt/verify jwt algo secret)
                        jwt))
                    :claims
                    (update :data (partial pserdes/deserialize data-serde) opts)))))))

(defmethod ig/init-key ::object-mapper [_ _]
  #?(:clj (jsonista/object-mapper
            {:encode-key-fn keywords/str
             :decode-key-fn keyword})))

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

(defn find-serde
  ([serdes type]
   (find-serde serdes type nil))
  ([serdes type default]
   (loop [[[_ serde] :as serdes] (seq serdes)]
     (cond
       (empty? serdes) default
       (string/starts-with? type (mime-type serde)) serde
       :else (recur (rest serdes))))))

(defn find-serde! [serdes type]
  (let [default-serde (or (:default serdes)
                          (:edn serdes)
                          (some-> serdes first val)
                          (throw (ex-info "could not find serializer" {})))]
    (find-serde serdes type default-serde)))
