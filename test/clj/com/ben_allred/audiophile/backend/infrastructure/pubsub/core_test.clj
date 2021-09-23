(ns ^:unit com.ben-allred.audiophile.backend.infrastructure.pubsub.core-test
  (:require
    [clojure.test :refer [are deftest is testing]]
    [com.ben-allred.audiophile.backend.infrastructure.pubsub.core :as ps]
    [com.ben-allred.audiophile.common.core.utils.colls :as colls]
    [com.ben-allred.audiophile.common.core.utils.uuids :as uuids]
    [com.ben-allred.audiophile.common.infrastructure.pubsub.protocols :as ppubsub]
    [test.utils.stubs :as stubs]))


(deftest emit-event!-test
  (testing "(emit-event!)"
    (let [pubsub (stubs/create (reify
                                 ppubsub/IPub
                                 (publish! [_ _ _])))
          [user-id model-id request-id] (repeatedly uuids/random)
          event-id (ps/emit-event! pubsub user-id model-id :event/type {:some :data} {:request/id request-id})]
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

(deftest emit-command!-test
  (testing "(emit-command!)"
    (let [pubsub (stubs/create (reify
                                 ppubsub/IPub
                                 (publish! [_ _ _])))
          [user-id request-id] (repeatedly uuids/random)
          event-id (ps/emit-command! pubsub user-id :command/type {:some :data} {:request/id request-id})]
      (testing "publishes an event"
        (let [[topic [k v ctx]] (colls/only! (stubs/calls pubsub :publish!))]
          (is (= [::ps/user user-id] topic))
          (is (= k event-id))
          (is (= {:command/id         event-id
                  :command/type       :command/type
                  :command/data       {:some :data}
                  :command/emitted-by user-id}
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
          event-id (ps/command-failed! pubsub model-id {:request/id    request-id
                                                        :user/id       user-id
                                                        :error/command :some/command!
                                                        :error/reason  "reason"})]
      (testing "publishes an event"
        (let [[topic [k v ctx]] (colls/only! (stubs/calls pubsub :publish!))]
          (is (= [::ps/user user-id] topic))
          (is (= k event-id))
          (is (= {:event/id         event-id
                  :event/model-id   model-id
                  :event/type       :command/failed
                  :event/data       {:error/command :some/command!
                                     :error/reason  "reason"}
                  :event/emitted-by user-id}
                 v))
          (is (= {:request/id request-id
                  :user/id    user-id}
                 ctx)))))))
