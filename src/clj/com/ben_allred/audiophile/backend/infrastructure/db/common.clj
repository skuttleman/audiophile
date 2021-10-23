(ns com.ben-allred.audiophile.backend.infrastructure.db.common
  (:require
    [com.ben-allred.audiophile.backend.api.repositories.core :as repos]
    [com.ben-allred.audiophile.backend.api.repositories.events.core :as events]
    [com.ben-allred.audiophile.backend.domain.interactors.protocols :as pint]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]))

(defn access? [executor query]
  (boolean (seq (repos/execute! executor query))))

(deftype DBMessageHandler [repo]
  pint/IMessageHandler
  (handle? [_ _]
    true)
  (handle! [this {event-id :event/id :event/keys [ctx] :as event}]
    (log/with-ctx [this :CP]
      (log/info "saving event to db" event-id)
      (repos/transact! repo events/insert-event! event ctx))))

(defn event->db-handler [{:keys [repo]}]
  (->DBMessageHandler repo))
