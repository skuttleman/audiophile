(ns com.ben-allred.audiophile.api.services.repositories.common
  (:require
    [com.ben-allred.audiophile.api.services.repositories.core :as repos]
    [com.ben-allred.audiophile.api.services.repositories.entities.core :as entities]
    [com.ben-allred.audiophile.api.services.repositories.protocols :as prepos]
    [com.ben-allred.audiophile.common.utils.colls :as colls]
    [com.ben-allred.audiophile.common.utils.logger :as log]
    [integrant.core :as ig]))

(deftype EntityExecutor [executor entity]
  prepos/IExecute
  (exec-raw! [_ sql opts]
    (repos/exec-raw! executor sql opts))
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
