(ns ^:unit com.ben-allred.audiophile.api.services.pubsub.ws-test
  (:require
    [clojure.test :refer [are deftest is testing]]
    [com.ben-allred.audiophile.api.services.pubsub.ws :as ws]
    [com.ben-allred.audiophile.api.services.pubsub.protocols :as pws]
    [com.ben-allred.audiophile.common.services.pubsub.core :as pubsub]
    [com.ben-allred.audiophile.common.services.serdes.protocols :as pserdes]
    [com.ben-allred.audiophile.common.utils.logger :as log]
    [test.utils.stubs :as stubs]))

(deftest ->handler-test
  (let [pubsub (pubsub/pubsub {})
        ->handler (ws/->handler {:heartbeat-int-ms 100
                                 :pubsub           pubsub
                                 :serdes           {:foo/bar (reify
                                                               pserdes/ISerde
                                                               (serialize [_ value _]
                                                                 [:serialized value])
                                                               (deserialize [_ value _]
                                                                 (second value))

                                                               pserdes/IMime
                                                               (mime-type [_]
                                                                 "foo/bar"))}})
        request {:user/id      ::user-id
                 :content-type "foo/bar"}
        stub (stubs/create (reify pws/IChannel
                             (open? [_] ::open?)
                             (send! [_ _])
                             (close! [_] ::close!)))]
    (testing "->handler"
      (let [handler (->handler request stub)]
        (testing "#on-open"
          (ws/on-open handler)
          (Thread/sleep 300)
          (testing "establishes a heartbeat"
            (let [msgs (frequencies (map first (stubs/calls stub :send!)))]
              (is (<= 2 (get msgs [:conn/ping])))))

          (testing "subscribes to broadcasts"
            (ws/broadcast! pubsub "event-id" {:some :event})
            (let [msgs (into #{}
                             (comp (map first)
                                   (remove (comp #{:conn/ping :conn/pong} first)))
                             (stubs/calls stub :send!))]
              (is (contains? msgs [:event/broadcast "event-id" {:some :event}]))))

          (testing "subscribes to user-level messages"
            (ws/send-user! pubsub ::user-id "event-id" {:some :event})
            (let [msgs (into #{}
                             (comp (map first)
                                   (remove (comp #{:conn/ping :conn/pong} first)))
                             (stubs/calls stub :send!))]
              (is (contains? msgs [:event/user "event-id" {:some :event} {:user/id ::user-id}]))))

          (testing "is not subscribed to other topics"
            (stubs/init! stub)
            (ws/send-user! pubsub ::unknown "event-id" {:some :event})
            (let [msgs (into #{}
                             (comp (map first)
                                   (remove (comp #{:conn/ping :conn/pong} first)))
                             (stubs/calls stub :send!))]
              (is (empty? msgs)))))
        (testing "#on-message"
          (testing "responds to keep-alive message"
            (ws/on-message handler [:serialized [:conn/ping]])
            (let [msgs (into #{} (map first) (stubs/calls stub :send!))]
              (is (contains? msgs [:conn/pong])))))

        (testing "#on-close"
          (ws/on-close handler)
          (stubs/init! stub)
          (stubs/set-stub! stub :open? false)
          (testing "unsubscribes from topics"
            (ws/broadcast! pubsub "event-id" {:another :event})
            (ws/send-user! pubsub ::user-id "event-id" {:another :event})
            (is (empty? (stubs/calls stub :send!))))

          (testing "stops sending heartbeats"
            (Thread/sleep 300)
            (is (empty? (stubs/calls stub :send!)))))))))

(deftest ->channel-test
  (let [->channel (ws/->channel {:serdes {:foo/bar (reify
                                                     pserdes/ISerde
                                                     (serialize [_ value _]
                                                       [:serialized value])
                                                     (deserialize [_ value _]
                                                       [:deserialized value])

                                                     pserdes/IMime
                                                     (mime-type [_]
                                                       "foo/bar"))}})
        request {:accept "foo/bar"}
        stub (stubs/create (reify pws/IChannel
                             (open? [_] ::open?)
                             (send! [_ _])
                             (close! [_] ::close!)))]
    (testing "->channel"
      (let [channel (->channel request stub)]
        (testing "#open?"
          (is (= ::open? (ws/open? channel))))

        (testing "#send!"
          (ws/send! channel ::msg)
          (let [[[msg] :as calls] (stubs/calls stub :send!)]
            (testing "send the serialized message"
              (is (= 1 (count calls)))
              (is (= [:serialized ::msg] msg)))))

        (testing "#close!"
          (is (= ::close! (ws/close! channel)))

          (testing "when the underlying channel throws an exception"
            (stubs/set-stub! stub :close! (fn [] (throw (Exception.))))
            (testing "returns nil"
              (is (nil? (ws/close! channel))))))))))
