(ns audiophile.backend.api.repositories.events.queries
  (:require
    [audiophile.backend.api.repositories.common :as crepos]
    [audiophile.backend.api.repositories.core :as repos]
    [audiophile.backend.infrastructure.db.models.core :as models]
    [audiophile.backend.infrastructure.db.models.tables :as tbl]
    [audiophile.common.core.utils.colls :as colls]
    [audiophile.common.core.utils.logger :as log]
    [audiophile.common.domain.validations.core :as val]))

(defn ^:private select-event-type-id [event-type]
  (-> tbl/event-types
      (models/select-fields #{:id})
      (models/select* [:and
                       [:= :event-types.category (namespace event-type)]
                       [:= :event-types.name (name event-type)]])))

(defn ^:private conform-fn [row]
  (let [event-type (keyword (:event/event-type row))
        ns (keyword (namespace event-type))
        spec (get-in tbl/models [ns :spec])]
    (-> row
        (assoc :event/event-type event-type)
        (update :event/data (partial val/conform spec)))))

(defn insert-event! [executor event opts]
  (let [event (assoc event
                     :emitted-by (:user/id opts)
                     :event-type-id (select-event-type-id (:event/type event)))]
    (-> tbl/events
        (models/insert-into event)
        (->> (repos/execute! executor))
        colls/only!
        :id)))

(defn select-for-user [executor user-id opts]
  (let [since (:filter/since opts)]
    (-> tbl/user-events
        (models/remove-fields #{:user-id})
        (models/select* (cond->> [:= :user-events.user-id user-id]
                          since (conj [:and [:>=
                                             :user-events.emitted-at
                                             (-> tbl/events
                                                 (models/select-fields #{:emitted-at})
                                                 (models/select* [:= :events.id since]))]
                                       [:not= :user-events.id since]])))
        (models/order-by [:user-events.emitted-at :desc])
        (as-> $query (repos/execute! executor
                                     $query
                                     {:model-fn     (crepos/->model-fn tbl/user-events)
                                      :result-xform (map conform-fn)})))))
