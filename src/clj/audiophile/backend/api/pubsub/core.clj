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

(defn ->ctx [ctx]
  (into {} (filter (comp #{"id"} name key)) ctx))

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
