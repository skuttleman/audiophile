(ns audiophile.backend.api.repositories.common
  (:require
    [audiophile.backend.api.repositories.core :as repos]
    [audiophile.backend.api.repositories.protocols :as prepos]
    [audiophile.common.core.serdes.core :as serdes]
    [audiophile.common.core.serdes.impl :as serde]
    [audiophile.common.core.utils.logger :as log]))

(defn ->model-fn [model]
  (fn [[k v]]
    (let [k' (keyword (name k))
          cast-fn (if-let [cast (get-in model [:casts k'])]
                    (case cast
                      :jsonb (partial serdes/deserialize serde/json)
                      :numrange #(some->> % .getValue (serdes/deserialize serde/edn))
                      keyword)
                    identity)]
      [k (cast-fn v)])))

(deftype Repository [tx ->executor]
  prepos/ITransact
  (transact! [_ f]
    (repos/transact! tx (comp f ->executor))))

(defn repo
  "Constructor for [[Repository]] used for running processes inside a transaction."
  [{:keys [->executor tx]}]
  (->Repository tx ->executor))

(deftype KVStore [client stream-serde]
  prepos/IKVStore
  (uri [_ key opts]
    (prepos/uri client key opts))
  (get [_ key opts]
    (serdes/deserialize stream-serde (prepos/get client key opts)))
  (put! [_ key value opts]
    (prepos/put! client key (serdes/serialize stream-serde value) opts)))

(defn store
  "Constructor for [[KVStore]] used to store and retrieve binary objects."
  [{:keys [client stream-serde]}]
  (->KVStore client stream-serde))