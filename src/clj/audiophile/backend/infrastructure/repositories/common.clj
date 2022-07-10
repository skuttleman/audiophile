(ns audiophile.backend.infrastructure.repositories.common
  (:require
    [audiophile.backend.api.pubsub.core :as ps]
    [audiophile.backend.infrastructure.repositories.protocols :as prepos]
    [audiophile.backend.infrastructure.templates.workflows :as wf]
    [audiophile.common.core.serdes.core :as serdes]
    [audiophile.common.core.serdes.impl :as serde]
    [audiophile.common.core.utils.logger :as log]
    [audiophile.common.core.utils.uuids :as uuids]
    [spigot.controllers.kafka.core :as sp.kafka]))

(defn ->model-fn [model]
  (fn [[k v]]
    (let [k' (keyword (name k))
          cast-fn (if-let [cast (get-in model [:casts k'])]
                    (case cast
                      :custom/edn (partial serdes/deserialize serde/edn)
                      :jsonb (partial serdes/deserialize serde/json)
                      :numrange #(some->> % .getValue (serdes/deserialize serde/edn))
                      keyword)
                    identity)]
      [k (cast-fn v)])))

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

(defn start-workflow! [producer template ctx opts]
  (let [workflow-id (uuids/random)
        wf (wf/workflow-spec template ctx)
        wf-ctx (assoc opts :workflow/id workflow-id)]
    (ps/send! producer {:key   workflow-id
                        :value (sp.kafka/create-wf-msg wf wf-ctx)})))
