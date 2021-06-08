(ns com.ben-allred.audiophile.backend.app.repositories.common
  (:require
    [com.ben-allred.audiophile.backend.app.repositories.core :as repos]
    [com.ben-allred.audiophile.backend.app.repositories.protocols :as prepos]
    [com.ben-allred.audiophile.common.core.serdes.core :as serdes]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]))

(defn ->model-fn [model]
  (fn [[k v]]
    (let [k' (keyword (name k))]
      [k (cond-> v
           (contains? (:casts model) k')
           keyword)])))

(deftype Repository [tx ->executor]
  prepos/ITransact
  (transact! [_ f]
    (repos/transact! tx (comp f ->executor))))

(defn repo [{:keys [->executor tx]}]
  (->Repository tx ->executor))

(deftype KVStore [client stream-serde]
  prepos/IKVStore
  (uri [_ key opts]
    (prepos/uri client key opts))
  (get [_ key opts]
    (serdes/deserialize stream-serde (prepos/get client key opts)))
  (put! [_ key value opts]
    (prepos/put! client key (serdes/serialize stream-serde value) opts)))

(defn store [{:keys [client stream-serde]}]
  (->KVStore client stream-serde))
