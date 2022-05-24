(ns audiophile.backend.infrastructure.db.common
  (:require
    [audiophile.backend.api.repositories.core :as repos]
    [audiophile.backend.api.repositories.events.core :as events]
    [audiophile.backend.domain.interactors.protocols :as pint]
    [audiophile.common.core.utils.logger :as log]))

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
