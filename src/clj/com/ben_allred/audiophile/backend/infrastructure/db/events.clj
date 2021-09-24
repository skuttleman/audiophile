(ns com.ben-allred.audiophile.backend.infrastructure.db.events
  (:require
    [com.ben-allred.audiophile.backend.api.repositories.common :as crepos]
    [com.ben-allred.audiophile.backend.api.repositories.core :as repos]
    [com.ben-allred.audiophile.backend.api.repositories.events.protocols :as pe]
    [com.ben-allred.audiophile.backend.infrastructure.db.models.core :as models]
    [com.ben-allred.audiophile.common.core.utils.colls :as colls]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.common.domain.validations.core :as val]))

(defn ^:private select-event-type-id [event-types event-type]
  (-> event-types
      (models/select-fields #{:id})
      (models/select* [:and
                       [:= :event-types.category (namespace event-type)]
                       [:= :event-types.name (name event-type)]])))

(defn ->conform-fn [models]
  (fn [row]
    (let [event-type (keyword (:event/event-type row))
          ns (keyword (namespace event-type))
          spec (get-in models [ns :spec])]
      (-> row
          (assoc :event/event-type event-type)
          (update :event/data (partial val/conform spec))))))

(deftype EventsExecutor [executor event-types events user-events conform-fn]
  pe/IEventsExecutor
  (insert-event! [_ {event-type :event/type :as event} {user-id :user/id}]
    (let [event (assoc event
                       :emitted-by user-id
                       :event-type-id (select-event-type-id event-types event-type))]
      (-> events
          (models/insert-into event)
          (->> (repos/execute! executor))
          colls/only!
          :id)))

  (select-for-user [_ user-id {:filter/keys [since]}]
    (-> user-events
        (models/remove-fields #{:user-id})
        (models/select* (cond->> [:= :user-events.user-id user-id]
                          since (conj [:and [:>=
                                             :user-events.emitted-at
                                             (-> events
                                                 (models/select-fields #{:emitted-at})
                                                 (models/select* [:= :events.id since]))]
                                       [:not= :user-events.id since]])))
        (models/order-by [:user-events.emitted-at :desc])
        (as-> $query (repos/execute! executor
                                     $query
                                     {:model-fn     (crepos/->model-fn user-events)
                                      :result-xform (map conform-fn)})))))

(defn ->executor
  "Factory constructor for [[EventsExecutor]] for interacting with the events repository."
  [{:keys [event-types events models user-events]}]
  (let [conform-fn (->conform-fn models)]
    (fn [executor]
      (->EventsExecutor executor event-types events user-events conform-fn))))
