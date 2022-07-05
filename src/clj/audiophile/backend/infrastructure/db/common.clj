(ns audiophile.backend.infrastructure.db.common
  (:require
    [audiophile.backend.infrastructure.repositories.core :as repos]
    [audiophile.backend.infrastructure.repositories.events.queries :as qevents]
    [audiophile.common.core.utils.logger :as log]))

(defn access? [executor query]
  (boolean (seq (repos/execute! executor query))))

(defn event-handler [{:keys [repo]}]
  (fn [{{event-id :event/id :event/keys [ctx] :as event} :value}]
    (log/with-ctx :CP
      (log/info "saving event to db" event-id)
      (repos/transact! repo qevents/insert-event! event ctx))))
