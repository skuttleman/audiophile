(ns com.ben-allred.audiophile.api.services.repositories.common
  (:require
    [com.ben-allred.audiophile.api.services.repositories.core :as repos]
    [com.ben-allred.audiophile.api.services.repositories.protocols :as prepos]
    [com.ben-allred.audiophile.common.services.serdes.core :as serdes]
    [com.ben-allred.audiophile.common.utils.logger :as log]
    [integrant.core :as ig]))

(defn ->model-fn [model]
  (fn [[k v]]
    (let [k' (keyword (name k))]
      [k (cond-> v
           (contains? (:casts model) k')
           keyword)])))

(deftype Repository [tx opts*]
  prepos/ITransact
  (transact! [_ f]
    (repos/transact! tx (fn [executor opts]
                          (f executor (merge opts opts*))))))

(defmethod ig/init-key ::repo [_ {:keys [opts tx]}]
  (->Repository tx opts))

(deftype KVStore [client stream-serde]
  prepos/IKVStore
  (uri [_ key opts]
    (prepos/uri client key opts))
  (get [_ key opts]
    (serdes/deserialize stream-serde (prepos/get client key opts)))
  (put! [_ key value opts]
    (prepos/put! client key (serdes/serialize stream-serde value) opts)))

(defmethod ig/init-key ::store [_ {:keys [s3-client stream-serde]}]
  (->KVStore s3-client stream-serde))
