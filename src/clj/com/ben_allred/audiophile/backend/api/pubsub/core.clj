(ns com.ben-allred.audiophile.backend.api.pubsub.core
  (:require
    [com.ben-allred.audiophile.backend.api.pubsub.protocols :as pps]
    [com.ben-allred.audiophile.common.api.pubsub.core :as pubsub]
    [com.ben-allred.audiophile.common.core.utils.maps :as maps]
    [com.ben-allred.audiophile.common.core.utils.uuids :as uuids]))

(defn open? [ch]
  (pps/open? ch))

(defn send! [ch msg]
  (pps/send! ch msg)
  nil)

(defn close! [ch]
  (pps/close! ch)
  nil)

(defn chan [conn opts]
  (pps/chan conn opts))

(defn subscribe! [ch handler opts]
  (pps/subscribe! ch handler opts)
  nil)

(defn ^:private ->ctx [ctx]
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

(defn emit-event! [ch model-id event-type data {user-id :user/id :as ctx}]
  (let [event-id (uuids/random)
        event {:event/id         event-id
               :event/model-id   model-id
               :event/type       event-type
               :event/data       data
               :event/emitted-by user-id
               :event/ctx        ctx}]
    (send! ch event)
    event-id))

(defn emit-command! [ch command-type data {user-id :user/id :as ctx}]
  (let [command-id (uuids/random)
        command {:command/id         command-id
                 :command/type       command-type
                 :command/data       data
                 :command/emitted-by user-id
                 :command/ctx        ctx}]
    (send! ch command)
    command-id))

(defn command-failed! [ch model-id opts]
  (let [[data ctx] (maps/extract-keys opts #{:error/command :error/reason})]
    (emit-event! ch model-id :command/failed data ctx)))
