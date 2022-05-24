(ns audiophile.common.core.serdes.core
  (:require
    [clojure.string :as string]
    [audiophile.common.core.serdes.protocols :as pserdes]
    [audiophile.common.core.utils.logger :as log]))

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
   (let [default (or default (:default serdes))]
     (if type
       (loop [[[_ serde] :as serdes] (seq serdes)]
         (cond
           (empty? serdes) default
           (string/starts-with? type (mime-type serde)) serde
           :else (recur (rest serdes))))
       default))))

(defn find-serde! [serdes type]
  (let [default-serde (or (:default serdes)
                          (some-> serdes first val)
                          (throw (ex-info "could not find serializer" {})))]
    (find-serde serdes type default-serde)))
