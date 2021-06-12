(ns com.ben-allred.audiophile.backend.infrastructure.db.events
  (:require
    [com.ben-allred.audiophile.backend.api.repositories.core :as repos]
    [com.ben-allred.audiophile.backend.api.repositories.events.protocols :as pe]
    [com.ben-allred.audiophile.backend.infrastructure.db.models.core :as models]
    [com.ben-allred.audiophile.common.core.utils.colls :as colls]))

(defn ^:private select-event-type-id [event-types event-type]
  (-> event-types
      (models/select-fields #{:id})
      (models/select* [:and
                       [:= :event-types.category (namespace event-type)]
                       [:= :event-types.name (name event-type)]])))

(deftype EventsExecutor [executor event-types events]
  pe/IEventsExecutor
  (insert-event! [_ event {user-id :user/id event-type :event/type}]
    (let [event (assoc event
                       :emitted-by user-id
                       :event-type-id (select-event-type-id event-types event-type))]
      (-> events
          (models/insert-into event)
          (->> (repos/execute! executor))
          colls/only!
          :id))))

(defn ->executor [{:keys [event-types events]}]
  (fn [executor]
    (->EventsExecutor executor event-types events)))
