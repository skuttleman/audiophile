(ns audiophile.backend.infrastructure.db.events
  (:require
    [audiophile.backend.api.repositories.common :as crepos]
    [audiophile.backend.api.repositories.core :as repos]
    [audiophile.backend.api.repositories.events.protocols :as pe]
    [audiophile.backend.infrastructure.db.models.core :as models]
    [audiophile.common.core.utils.colls :as colls]
    [audiophile.common.core.utils.logger :as log]
    [audiophile.common.domain.validations.core :as val]))

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

(defn ^:private events-executor#insert-event!
  [executor event-types events event opts]
  (let [event (assoc event
                     :emitted-by (:user/id opts)
                     :event-type-id (select-event-type-id event-types
                                                          (:event/type event)))]
    (-> events
        (models/insert-into event)
        (->> (repos/execute! executor))
        colls/only!
        :id)))

(defn ^:private events-executor#select-for-user
  [executor events user-events conform-fn user-id opts]
  (let [since (:filter/since opts)]
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

(deftype EventsExecutor [executor event-types events user-events conform-fn]
  pe/IEventsExecutor
  (insert-event! [_ event opts]
    (events-executor#insert-event! executor event-types events event opts))

  (select-for-user [_ user-id opts]
    (events-executor#select-for-user executor events user-events conform-fn user-id opts)))

(defn ->executor
  "Factory constructor for [[EventsExecutor]] for interacting with the events repository."
  [{:keys [event-types events models user-events]}]
  (let [conform-fn (->conform-fn models)]
    (fn [executor]
      (->EventsExecutor executor event-types events user-events conform-fn))))
