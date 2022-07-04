(ns audiophile.backend.infrastructure.db.common
  (:require
    [audiophile.backend.infrastructure.repositories.workflows.queries :as qworkflows]
    [audiophile.backend.infrastructure.repositories.core :as repos]
    [audiophile.backend.infrastructure.repositories.events.queries :as qevents]
    [audiophile.common.core.utils.logger :as log]))

(defn ^:private handle* [executor event ctx]
  (qevents/insert-event! executor event ctx)
  (qworkflows/update-by-id! executor
                            (:workflow/id ctx)
                            (case (:event/type event)
                              :command/failed {:status "failed"}
                              :workflow/completed {:status "completed" :data (:event/data event)})))

(defn access? [executor query]
  (boolean (seq (repos/execute! executor query))))

(defn event-handler [{:keys [repo]}]
  (fn [{{event-id :event/id :event/keys [ctx] :as event} :value}]
    (log/with-ctx :CP
      (log/info "saving event to db" event-id)
      (repos/transact! repo handle* event ctx))))
