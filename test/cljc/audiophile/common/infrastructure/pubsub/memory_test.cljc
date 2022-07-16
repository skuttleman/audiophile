(ns ^:unit audiophile.common.infrastructure.pubsub.memory-test
  (:require
    [clojure.core.async :as async]
    [clojure.test :refer [are deftest is testing]]
    [audiophile.common.api.pubsub.core :as pubsub]
    [audiophile.common.core.utils.colls :as colls]
    [audiophile.common.infrastructure.pubsub.memory :as pubsub.mem]
    [audiophile.test.utils :refer [async] :as tu]
    [audiophile.test.utils.spies :as spies]))

(deftest PubSub-test
  (testing "PubSub"
    (testing "with sync behavior"
      (let [pubsub (pubsub.mem/pubsub {})
            spy (spies/create)]
        (async done
          (async/go
            (pubsub/subscribe! pubsub ::id [:topic/one] spy)
            (pubsub/subscribe! pubsub ::id [:topic/two :a] spy)
            (pubsub/subscribe! pubsub ::id [:topic/two :b] spy)
            (testing "when subscribed to a topic"
              (testing "and when events are published to the topics"
                (pubsub/publish! pubsub [:topic/one] "event 1")
                (pubsub/publish! pubsub [:topic/two :a] "event 2")
                (pubsub/publish! pubsub [:topic/two :b] "event 3")
                (tu/<ch! (async/timeout 200))

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
                (tu/<ch! (async/timeout 200))

                (let [calls (set (spies/calls spy))]
                  (testing "receives the subscribed events"
                    (is (contains? calls [[:topic/one] "event 1"]))
                    (is (contains? calls [[:topic/two :b] "event 3"])))

                  (testing "does not receive other events"
                    (is (not (contains? calls [[:topic/two :a] "event 2"])))))))

            (testing "when resubscribing to topic"
              (spies/init! spy)
              (pubsub/subscribe! pubsub ::id [:topic/two :a] spy)
              (pubsub/publish! pubsub [:topic/two :a] "event 4")
              (tu/<ch! (async/timeout 200))

              (let [calls (set (spies/calls spy))]
                (testing "receives the subscribed event"
                  (is (contains? calls [[:topic/two :a] "event 4"])))))

            (testing "when unsubscribing from all topics"
              (spies/init! spy)
              (pubsub/unsubscribe! pubsub ::id)
              (testing "and when events are published to the topics"
                (pubsub/publish! pubsub [:topic/one] "event 1")
                (pubsub/publish! pubsub [:topic/two :a] "event 2")
                (pubsub/publish! pubsub [:topic/two :b] "event 3")
                (tu/<ch! (async/timeout 200))

                (testing "does not receive any events"
                  (is (empty? (spies/calls spy))))))

            #?(:clj
               (testing "with async behavior"
                 (let [pubsub (pubsub.mem/pubsub {})
                       spies (repeatedly 5000 spies/create)]
                   (doseq [[idx spy] (map-indexed vector spies)]
                     (pubsub/subscribe! pubsub idx [:some/topic] spy))
                   (pubsub/publish! pubsub [:some/topic] {:an :event})
                   (is (some (comp empty? spies/calls)
                             spies))
                   (Thread/sleep 200)
                   (is (every? (comp #{[[:some/topic] {:an :event}]} colls/only! spies/calls)
                               spies)))))
            (done)))))))
