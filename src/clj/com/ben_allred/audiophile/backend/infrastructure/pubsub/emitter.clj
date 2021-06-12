(ns com.ben-allred.audiophile.backend.infrastructure.pubsub.emitter
  (:require
    [com.ben-allred.audiophile.backend.api.repositories.events.core :as events]
    [com.ben-allred.audiophile.backend.domain.interactors.protocols :as pint]
    [com.ben-allred.audiophile.backend.infrastructure.pubsub.ws :as ws]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]))

(deftype Emitter [executor pubsub]
  pint/IEmitter
  (command-failed! [_ command-id opts]
    (try
      (let [event (select-keys opts #{:error/reason})
            event-id (try
                       (events/insert-event! executor
                                             {:event/model-id command-id
                                              :event/data     event}
                                             (assoc opts :event/type :command/failed))
                       (catch Throwable ex
                         (log/error ex "error inserting event")))]
        (ws/send-user! pubsub
                       (:user/id opts)
                       event-id
                       {:event/data event
                        :event/type :command/failed}
                       {:request/id command-id}))
      (catch Throwable ex
        (log/error ex "error emitting command failed")))))

(defn ->emitter [{:keys [pubsub]}]
  (fn [executor]
    (->Emitter executor pubsub)))
