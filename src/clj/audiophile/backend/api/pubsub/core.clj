(ns audiophile.backend.api.pubsub.core
  (:require
    [audiophile.backend.api.pubsub.protocols :as pps]
    [audiophile.common.api.pubsub.core :as pubsub]
    [audiophile.common.core.utils.logger :as log]
    [audiophile.common.core.utils.uuids :as uuids]
    [spigot.controllers.kafka.core :as sp.kafka]))

(defn open? [ch]
  (pps/open? ch))

(defn send! [ch msg]
  (pps/send! ch msg)
  nil)

(defn close! [ch]
  (pps/close! ch)
  nil)

(defn ^:private ->ctx [ctx]
  (into {} (filter (comp #{"id"} name key)) ctx))

(defn generate-event [model-id event-type data {user-id :user/id :as ctx}]
  {:event/id         (uuids/random)
   :event/model-id   model-id
   :event/type       event-type
   :event/data       data
   :event/emitted-by user-id
   :event/ctx        (->ctx ctx)})

(defn broadcast!
  "Broadcast an event to all connections"
  ([pubsub event-id event]
   (broadcast! pubsub event-id event nil))
  ([pubsub event-id event ctx]
   (pubsub/publish! pubsub [::broadcast] [event-id event (->ctx ctx)])))

(defn send-user!
  "Broadcast an event to all connections for a specific user"
  ([pubsub user-id event-id event]
   (send-user! pubsub user-id event-id event nil))
  ([pubsub user-id msg-id msg ctx]
   (pubsub/publish! pubsub [::user user-id] [msg-id msg (assoc (->ctx ctx) :user/id user-id)])))

(defn send-workflow! [producer workflow-id wf opts]
  (send! producer {:key   workflow-id
                   :value (sp.kafka/create-wf-msg workflow-id wf (->ctx opts))}))
