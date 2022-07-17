(ns audiophile.backend.api.pubsub.core
  (:require
    [audiophile.backend.api.pubsub.protocols :as pps]
    [audiophile.common.api.pubsub.core :as pubsub]
    [audiophile.common.core.utils.logger :as log]))

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

(defn publish!
  "Publish an event"
  ([pubsub topic msg-id msg]
   (publish! pubsub topic msg-id msg nil))
  ([pubsub topic msg-id msg ctx]
   (pubsub/publish! pubsub topic [msg-id msg (->ctx ctx)])))

(defn broadcast!
  "Broadcast an event to all connections"
  ([pubsub event-id event]
   (broadcast! pubsub event-id event nil))
  ([pubsub event-id event ctx]
   (publish! pubsub [::broadcast] event-id event ctx)))

(defn send-user!
  "Broadcast an event to all connections for a specific user"
  ([pubsub user-id event-id event]
   (send-user! pubsub user-id event-id event nil))
  ([pubsub user-id event-id event ctx]
   (publish! pubsub [::user user-id] event-id event (assoc ctx :user/id user-id))))
