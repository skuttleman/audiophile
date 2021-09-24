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
  (handle! [_ {[event-id event ctx] :msg :as msg}]
    (try
      (log/info "saving event to db" event-id)
      (repos/transact! repo events/insert-event! event ctx)
      (catch Throwable ex
        (log/error ex "failed: saving event to db" msg)))))

(defn event->db-handler [{:keys [repo]}]
  (->DBMessageHandler repo))
