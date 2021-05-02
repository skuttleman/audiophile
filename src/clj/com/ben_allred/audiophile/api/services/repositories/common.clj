(ns com.ben-allred.audiophile.api.services.repositories.common
  (:require
    [com.ben-allred.audiophile.api.services.repositories.core :as repos]
    [com.ben-allred.audiophile.api.services.repositories.entities.core :as entities]
    [com.ben-allred.audiophile.api.services.repositories.protocols :as prepos]
    [com.ben-allred.audiophile.common.services.serdes.core :as serdes]
    [com.ben-allred.audiophile.common.utils.colls :as colls]
    [com.ben-allred.audiophile.common.utils.logger :as log]
    [integrant.core :as ig]))

(deftype EntityExecutor [executor entity]
  prepos/IExecute
  (execute! [_ query opts]
    (repos/execute! executor query (assoc opts
                                          :entity-fn
                                          (fn [[k v]]
                                            (let [k' (keyword (name k))]
                                              [k (cond-> v
                                                   (contains? (:casts entity) k')
                                                   keyword)]))))))

(deftype DefaultRepository [tx entity opts-key]
  prepos/ITransact
  (transact! [_ f]
    (repos/transact! tx (fn [executor opts]
                          (f (->EntityExecutor executor entity)
                             (assoc opts opts-key entity))))))

(defmethod ig/init-key ::repo [_ {:keys [entity tx]}]
  (->DefaultRepository tx entity (keyword "entity" (name (:table entity)))))

(deftype KVBackedRepository [tx kv-store]
  prepos/ITransact
  (transact! [_ f]
    (repos/transact! tx (fn [executor opts]
                          (f executor (assoc opts :store/kv kv-store))))))

(defmethod ig/init-key ::kv-repo [_ {:keys [kv-store tx]}]
  (->KVBackedRepository tx kv-store))

(deftype SerdeStore [s3-client serdes]
  prepos/IKVStore
  (uri [_ key opts]
    (prepos/uri s3-client key opts))
  (get [_ key opts]
    (let [result (prepos/get s3-client key opts)
          serde (serdes/find-serde serdes (:ContentType result))]
      (update result :Body (partial serdes/deserialize serde))))
  (put! [_ key value opts]
    (let [serde (serdes/find-serde serdes (:content-type opts))]
      (prepos/put! s3-client key value #_(serdes/serialize serde value opts) opts))))

(defmethod ig/init-key ::s3-store [_ {:keys [s3-client serdes]}]
  (->SerdeStore s3-client serdes))

(defn query-many [repo entity-key clause]
  (repos/transact! repo
                   repos/->exec!
                   repos/execute!
                   (fn [{entity entity-key}]
                     (entities/select* entity clause))))

(defn query-one [repo entity-key clause]
  (colls/only! (query-many repo entity-key clause)))

(defn create* [executor entity row]
  (->> row
       (entities/insert-into entity)
       (repos/execute! executor)
       first
       :id))

(defn create! [repo entity-id row]
  (repos/transact! repo (fn [executor {entity entity-id}]
                          (->> row
                               (create* executor entity)
                               (conj [:= :id])
                               (entities/select* entity)
                               (repos/execute! executor)
                               colls/only!))))
