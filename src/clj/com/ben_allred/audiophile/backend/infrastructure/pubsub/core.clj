(ns com.ben-allred.audiophile.backend.infrastructure.pubsub.core
  (:require
    [com.ben-allred.audiophile.backend.infrastructure.pubsub.protocols :as pps]
    [com.ben-allred.audiophile.common.core.utils.uuids :as uuids]
    [com.ben-allred.audiophile.common.infrastructure.pubsub.core :as pubsub]))

(defn open? [ch]
  (pps/open? ch))

(defn send! [ch msg]
  (pps/send! ch msg)
  nil)

(defn close! [ch]
  (pps/close! ch)
  nil)

(defn on-open [handler]
  (pps/on-open handler))

(defn on-message [handler msg]
  (pps/on-message handler msg))

(defn on-close [handler]
  (pps/on-close handler))

(defn chan [conn opts]
  (pps/chan conn opts))

(defn subscribe! [ch handler opts]
  (pps/subscribe! ch handler opts)
  nil)

(defn ^:private ->ctx [ctx]
  (some-> ctx (select-keys #{:request/id :user/id})))

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

(defn emit-event! [pubsub user-id model-id event-type data ctx]
  (let [event-id (uuids/random)
        event {:event/id         event-id
               :event/model-id   model-id
               :event/type       event-type
               :event/data       data
               :event/emitted-by user-id}]
    (send-user! pubsub user-id event-id event ctx)
    event-id))

(defn command-failed! [pubsub model-id opts]
  (emit-event! pubsub
               (:user/id opts)
               model-id
               :command/failed
               (select-keys opts #{:error/command :error/reason})
               opts))

(defn emit-command! [pubsub user-id command-type data ctx]
  (let [command-id (uuids/random)
        event {:command/id         command-id
               :command/type       command-type
               :command/data       data
               :command/emitted-by user-id}]
    (send-user! pubsub user-id command-id event ctx)
    command-id))
