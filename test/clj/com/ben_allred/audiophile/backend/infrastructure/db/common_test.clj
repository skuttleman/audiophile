(ns com.ben-allred.audiophile.backend.infrastructure.db.common-test
  (:require
    [clojure.test :refer [are deftest is testing]]
    [com.ben-allred.audiophile.backend.infrastructure.db.common :as cdb]
    [com.ben-allred.audiophile.backend.infrastructure.db.events :as db.events]
    [com.ben-allred.audiophile.backend.infrastructure.pubsub.core :as ps]
    [com.ben-allred.audiophile.common.core.utils.colls :as colls]
    [com.ben-allred.audiophile.common.core.utils.uuids :as uuids]
    [com.ben-allred.audiophile.common.infrastructure.pubsub.protocols :as ppubsub]
    [test.utils.stubs :as stubs]
    [test.utils.repositories :as trepos]
    [com.ben-allred.audiophile.common.core.utils.fns :as fns]
    [test.utils :as tu]))

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

(deftest emit!-test
  (testing "(emit!)"
    (let [pubsub (stubs/create (reify
                                 ppubsub/IPub
                                 (publish! [_ _ _])))
          [user-id model-id request-id] (repeatedly uuids/random)
          event-id (cdb/emit! pubsub user-id model-id :event/type {:some :data} {:request/id request-id})]
      (testing "publishes an event"
        (let [[topic [k v ctx]] (colls/only! (stubs/calls pubsub :publish!))]
          (is (= [::ps/user user-id] topic))
          (is (= k event-id))
          (is (= {:event/id         event-id
                  :event/model-id   model-id
                  :event/type       :event/type
                  :event/data       {:some :data}
                  :event/emitted-by user-id}
                 v))
          (is (= {:request/id request-id
                  :user/id    user-id}
                 ctx)))))))

(deftest command-failed!-test
  (testing "(command-failed!)"
    (let [pubsub (stubs/create (reify
                                 ppubsub/IPub
                                 (publish! [_ _ _])))
          [user-id model-id request-id] (repeatedly uuids/random)
          event-id (cdb/command-failed! pubsub model-id {:request/id    request-id
                                                         :user/id       user-id
                                                         :error/command :some/command
                                                         :error/reason  "reason"})]
      (testing "publishes an event"
        (let [[topic [k v ctx]] (colls/only! (stubs/calls pubsub :publish!))]
          (is (= [::ps/user user-id] topic))
          (is (= k event-id))
          (is (= {:event/id         event-id
                  :event/model-id   model-id
                  :event/type       :command/failed
                  :event/data       {:error/command :some/command
                                     :error/reason  "reason"}
                  :event/emitted-by user-id}
                 v))
          (is (= {:request/id request-id
                  :user/id    user-id}
                 ctx)))))))

(deftest db-handler-test
  (testing "(db-handler)"
    (let [repo (trepos/stub-transactor ->event-executor)
          handler (cdb/db-handler {:repo repo})
          user-id (uuids/random)]
      (handler {:event [:_ {:user/id    user-id
                            :event/type :some/event
                            :some       :data}]})
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
