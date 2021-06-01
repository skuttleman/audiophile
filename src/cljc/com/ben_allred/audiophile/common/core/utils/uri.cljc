(ns com.ben-allred.audiophile.common.core.utils.uri
  (:require
    [clojure.string :as string]
    [com.ben-allred.audiophile.common.core.utils.maps :as maps]
    [lambdaisland.uri :as uri*])
  #?(:clj
     (:import
       (java.net URLEncoder URLDecoder))))

(defn ^:private url-encode
  "Encode a parameter to be used in a url"
  [arg]
  #?(:clj  (URLEncoder/encode ^String arg "UTF-8")
     :cljs (js/encodeURIComponent (str arg))))

(defn ^:private url-decode
  "Decode a parameter used in a url"
  [arg]
  (-> arg
      str
      (string/replace #"\+" " ")
      #?(:clj  (URLDecoder/decode "UTF-8")
         :cljs js/decodeURIComponent)))

(defn join-query
  "Join a map into a query-string representation"
  [arg]
  (when (seq arg)
    (string/join \& (keep (fn [[k v]]
                            (if (vector? v)
                              (join-query (map (fn [v] [k v]) v))
                              (when v
                                (apply str
                                       (url-encode (name k))
                                       (when-not (boolean? v)
                                         [\=
                                          (url-encode (str (cond-> v (keyword? v) name)))])))))
                          arg))))

(defn split-query
  "Split a query-string into a map representation"
  [query-string]
  (when (seq query-string)
    (->> (string/split query-string #"&")
         (map (fn [pair]
                (let [[k v] (string/split pair #"=")]
                  [(keyword k) (if (nil? v) true (url-decode v))])))
         (reduce (fn [m [k v]]
                   (if-some [v' (get m k)]
                     (if (vector? v')
                       (update m k conj v)
                       (assoc m k [v' v]))
                     (assoc m k v)))
                 {}))))

(defn parse
  "Parse a uri string into a record"
  [uri]
  (-> uri
      uri*/parse
      (maps/update-maybe :query split-query)))

(defn stringify
  "Convert a uri record into a string"
  [uri]
  (-> uri
      (maps/update-maybe :query join-query)
      str))
