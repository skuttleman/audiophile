(ns com.ben-allred.audiophile.common.utils.uri
  (:require
    [clojure.string :as string]
    [com.ben-allred.audiophile.common.utils.maps :as maps]
    [lambdaisland.uri :as uri*])
  #?(:clj
     (:import
       (java.net URLEncoder URLDecoder))))

(defn url-encode [arg]
  #?(:clj  (URLEncoder/encode ^String arg "UTF-8")
     :cljs (js/encodeURIComponent (str arg))))

(defn url-decode [arg]
  (-> arg
      str
      (string/replace #"\+" " ")
      #?(:clj  (URLDecoder/decode "UTF-8")
         :cljs js/decodeURIComponent)))

(defn join-query [arg]
  (when (seq arg)
    (string/join \& (map (fn [[k v]]
                           (if (vector? v)
                             (join-query (map (fn [v] [k v]) v))
                             (apply str
                                    (url-encode (name k))
                                    (when-not (boolean? v)
                                      [\=
                                       (url-encode (name v))]))))
                         arg))))

(defn split-query [query-string]
  (when (seq query-string)
    (into {}
          (map (fn [pair]
                 (let [[k v] (string/split pair #"=")]
                   [(keyword k) (if (nil? v) true (url-decode v))])))
          (string/split query-string #"&"))))

(defn parse [uri]
  (-> uri
      uri*/parse
      (maps/update-maybe :query split-query)))

(defn stringify [uri]
  (-> uri
      (update :query join-query)
      str))
