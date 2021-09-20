(ns com.ben-allred.audiophile.backend.infrastructure.db.common
  (:require
    [clojure.set :as set]
    [com.ben-allred.audiophile.backend.api.repositories.core :as repos]
    [com.ben-allred.audiophile.backend.api.repositories.events.core :as events]
    [com.ben-allred.audiophile.backend.infrastructure.pubsub.ws :as ws]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.common.core.utils.uuids :as uuids]
    [com.ben-allred.audiophile.common.infrastructure.pubsub.core :as pubsub]))

(defn emit! [pubsub user-id model-id event-type data ctx]
  (let [event-id (uuids/random)
        event {:event/id         event-id
               :event/model-id   model-id
               :event/type       event-type
               :event/data       data
               :event/emitted-by user-id}]
    (ws/send-user! pubsub user-id event-id event ctx)
    event-id))

(defn command-failed! [pubsub model-id opts]
  (emit! pubsub
         (:user/id opts)
         model-id
         :command/failed
         (select-keys opts #{:error/command :error/reason})
         opts))

(defn access? [executor query]
  (boolean (seq (repos/execute! executor query))))

(defn db-handler [{:keys [repo]}]
  (fn [_ {[_ event] :event}]
    (repos/transact! repo
                     events/insert-event!
                     event
                     (set/rename-keys event {:event/emitted-by :user/id}))))

(defn ws-handler [{:keys [pubsub]}]
  (fn [_ {:keys [topic event]}]
    (pubsub/publish! pubsub topic event)))
