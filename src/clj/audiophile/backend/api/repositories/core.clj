(ns audiophile.backend.api.repositories.core
  (:refer-clojure :exclude [get])
  (:require
    [audiophile.backend.api.repositories.protocols :as prepos]
    [audiophile.common.core.utils.logger :as log]))

(defn execute!
  ([executor query]
   (execute! executor query nil))
  ([executor query opts]
   (prepos/execute! executor query opts)))

(defn transact!
  ([transactor f]
   (prepos/transact! transactor f))
  ([transactor f & args]
   (prepos/transact! transactor #(apply f % args))))

(defn uri
  ([kv-store key]
   (uri kv-store key nil))
  ([kv-store key opts]
   (prepos/uri kv-store key opts)))

(defn get
  ([kv-store key]
   (get kv-store key nil))
  ([kv-store key opts]
   (prepos/get kv-store key opts)))

(defn put!
  ([kv-store key value]
   (put! kv-store key value nil))
  ([kv-store key value opts]
   (prepos/put! kv-store key value opts)))
