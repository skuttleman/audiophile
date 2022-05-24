(ns ^:unit audiophile.backend.infrastructure.db.common-test
  (:require
    [clojure.test :refer [are deftest is testing]]
    [audiophile.backend.domain.interactors.core :as int]
    [audiophile.backend.infrastructure.db.common :as cdb]
    [audiophile.backend.infrastructure.db.events :as db.events]
    [audiophile.common.core.utils.colls :as colls]
    [audiophile.common.core.utils.fns :as fns]
    [audiophile.common.core.utils.uuids :as uuids]
    [audiophile.test.utils :as tu]
    [audiophile.test.utils.repositories :as trepos]
    [audiophile.test.utils.stubs :as stubs]))

(defn ^:private ->event-executor
  ([config]
   (fn [executor models]
     (->event-executor executor (merge models config))))
  ([executor {:keys [events user-events event-types] :as models}]
   (let [->executor (db.events/->executor {:event-types event-types
                                           :events      events
                                           :models      models
                                           :user-events user-events})]
     (->executor executor))))

(deftest event->db-handler-test
  (testing "(event->db-handler)"
    (let [repo (trepos/stub-transactor ->event-executor)
          handler (cdb/event->db-handler {:repo repo})
          user-id (uuids/random)]
      (int/handle! handler
                   {:event/type :some/event
                    :some       :data
                    :event/ctx  {:user/id user-id}})
      (let [[query] (colls/only! (stubs/calls repo :execute!))
            value (colls/only! (:values query))]
        (is (= {:insert-into :events
                :returning   [:id]}
               (dissoc query :values)))
        (is (= {:emitted-by    user-id
                :event-type-id {:select #{[:event-types.id "event-type/id"]}
                                :from   [:event-types]
                                :where  [:and
                                         #{[:= #{:event-types.category "some"}]
                                           [:= #{:event-types.name "event"}]}]}}
               (update value
                       :event-type-id
                       (fns/=> (update :select set)
                               (update :where (fns/=> (update 1 tu/op-set)
                                                      (update 2 tu/op-set)
                                                      tu/op-set))))))))))
