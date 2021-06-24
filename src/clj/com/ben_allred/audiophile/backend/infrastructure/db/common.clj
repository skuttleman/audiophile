(ns com.ben-allred.audiophile.backend.infrastructure.db.common
  (:require
    [com.ben-allred.audiophile.backend.api.repositories.core :as repos]
    [com.ben-allred.audiophile.backend.api.repositories.events.core :as events]
    [com.ben-allred.audiophile.backend.infrastructure.pubsub.ws :as ws]))

(defn emit! [executor pubsub user-id id event-type data ctx]
  (let [event {:event/model-id id
               :event/data     data}
        event-id (events/insert-event! executor
                                       event
                                       {:event/type event-type
                                        :user/id    user-id})]
    (ws/send-user! pubsub
                   user-id
                   event-id
                   (assoc event
                          :event/id event-id
                          :event/type event-type
                          :event/emitted-by user-id)
                   ctx)
    event-id))

(defn access? [executor query]
  (boolean (seq (repos/execute! executor query))))
