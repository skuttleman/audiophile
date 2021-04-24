(ns ^:unit com.ben-allred.audiophile.common.services.pubsub.core-test
  (:require
    [clojure.test :refer [are deftest is testing]]
    [com.ben-allred.audiophile.common.services.pubsub.core :as pubsub]
    [test.utils.stubs :as stubs]))

(deftest PubSub-test
  (testing "PubSub"
    (let [pubsub (pubsub/->PubSub (atom nil))
          stub (stubs/create)]
      (pubsub/subscribe! pubsub ::id [:topic/one] stub)
      (pubsub/subscribe! pubsub ::id [:topic/two :a] stub)
      (pubsub/subscribe! pubsub ::id [:topic/two :b] stub)
      (testing "when subscribed to a topic"
        (testing "and when events are published to the topics"
          (pubsub/publish! pubsub [:topic/one] "event 1")
          (pubsub/publish! pubsub [:topic/two :a] "event 2")
          (pubsub/publish! pubsub [:topic/two :b] "event 3")

          (testing "receives the events"
            (let [calls (set (stubs/calls stub))]
              (is (contains? calls [[:topic/one] "event 1"]))
              (is (contains? calls [[:topic/two :a] "event 2"]))
              (is (contains? calls [[:topic/two :b] "event 3"]))))))

      (testing "when unsubscribing from a topic"
        (stubs/init! stub)
        (pubsub/unsubscribe! pubsub ::id [:topic/two :a])
        (testing "and when events are published to the topics"
          (pubsub/publish! pubsub [:topic/one] "event 1")
          (pubsub/publish! pubsub [:topic/two :a] "event 2")
          (pubsub/publish! pubsub [:topic/two :b] "event 3")

          (let [calls (set (stubs/calls stub))]
            (testing "receives the subscribed events"
              (is (contains? calls [[:topic/one] "event 1"]))
              (is (contains? calls [[:topic/two :b] "event 3"])))

            (testing "does not receive other events"
              (is (not (contains? calls [[:topic/two :a] "event 2"])))))))

      (testing "when unsubscribing from all topics"
        (stubs/init! stub)
        (pubsub/unsubscribe! pubsub ::id)
        (testing "and when events are published to the topics"
          (pubsub/publish! pubsub [:topic/one] "event 1")
          (pubsub/publish! pubsub [:topic/two :a] "event 2")
          (pubsub/publish! pubsub [:topic/two :b] "event 3")

          (testing "does not receive any events"
            (is (empty? (stubs/calls stub)))))))))
