(ns ^:unit com.ben-allred.audiophile.common.services.pubsub.core-test
  (:require
    [clojure.test :refer [are deftest is testing]]
    [com.ben-allred.audiophile.common.services.pubsub.core :as pubsub]
    [test.utils.spies :as spies]))

(deftest PubSub-test
  (testing "PubSub"
    (let [pubsub (pubsub/->PubSub (atom nil))
          spy (spies/create)]
      (pubsub/subscribe! pubsub ::id [:topic/one] spy)
      (pubsub/subscribe! pubsub ::id [:topic/two :a] spy)
      (pubsub/subscribe! pubsub ::id [:topic/two :b] spy)
      (testing "when subscribed to a topic"
        (testing "and when events are published to the topics"
          (pubsub/publish! pubsub [:topic/one] "event 1")
          (pubsub/publish! pubsub [:topic/two :a] "event 2")
          (pubsub/publish! pubsub [:topic/two :b] "event 3")

          (testing "receives the events"
            (let [calls (set (spies/calls spy))]
              (is (contains? calls [[:topic/one] "event 1"]))
              (is (contains? calls [[:topic/two :a] "event 2"]))
              (is (contains? calls [[:topic/two :b] "event 3"]))))))

      (testing "when unsubscribing from a topic"
        (spies/init! spy)
        (pubsub/unsubscribe! pubsub ::id [:topic/two :a])
        (testing "and when events are published to the topics"
          (pubsub/publish! pubsub [:topic/one] "event 1")
          (pubsub/publish! pubsub [:topic/two :a] "event 2")
          (pubsub/publish! pubsub [:topic/two :b] "event 3")

          (let [calls (set (spies/calls spy))]
            (testing "receives the subscribed events"
              (is (contains? calls [[:topic/one] "event 1"]))
              (is (contains? calls [[:topic/two :b] "event 3"])))

            (testing "does not receive other events"
              (is (not (contains? calls [[:topic/two :a] "event 2"])))))))

      (testing "when unsubscribing from all topics"
        (spies/init! spy)
        (pubsub/unsubscribe! pubsub ::id)
        (testing "and when events are published to the topics"
          (pubsub/publish! pubsub [:topic/one] "event 1")
          (pubsub/publish! pubsub [:topic/two :a] "event 2")
          (pubsub/publish! pubsub [:topic/two :b] "event 3")

          (testing "does not receive any events"
            (is (empty? (spies/calls spy)))))))))
