(ns ^:unit com.ben-allred.audiophile.backend.infrastructure.pubsub.ws-test
  (:require
    [clojure.test :refer [are deftest is testing]]
    [com.ben-allred.audiophile.backend.infrastructure.pubsub.ws :as ws]
    [com.ben-allred.audiophile.backend.infrastructure.pubsub.core :as ps]
    [com.ben-allred.audiophile.backend.infrastructure.pubsub.protocols :as pws]
    [com.ben-allred.audiophile.common.infrastructure.pubsub.memory :as pubsub.mem]
    [com.ben-allred.audiophile.common.core.serdes.protocols :as pserdes]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.test.utils.stubs :as stubs]))

(deftest ->handler-test
  (let [pubsub (pubsub.mem/pubsub {:sync? true})
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
          (ps/on-open handler)
          (Thread/sleep 300)
          (testing "establishes a heartbeat"
            (let [msgs (frequencies (map first (stubs/calls stub :send!)))]
              (is (<= 2 (get msgs [:conn/ping])))))

          (testing "subscribes to broadcasts"
            (ps/broadcast! pubsub "event-id" {:some :event})
            (let [msgs (into #{}
                             (comp (map first)
                                   (remove (comp #{:conn/ping :conn/pong} first)))
                             (stubs/calls stub :send!))]
              (is (contains? msgs [:event/broadcast "event-id" {:some :event} {}]))))

          (testing "subscribes to user-level messages"
            (ps/send-user! pubsub ::user-id "event-id" {:some :event})
            (let [msgs (into #{}
                             (comp (map first)
                                   (remove (comp #{:conn/ping :conn/pong} first)))
                             (stubs/calls stub :send!))]
              (is (contains? msgs [:event/user "event-id" {:some :event} {:user/id ::user-id}]))))

          (testing "is not subscribed to other topics"
            (stubs/init! stub)
            (ps/send-user! pubsub ::unknown "event-id" {:some :event})
            (let [msgs (into #{}
                             (comp (map first)
                                   (remove (comp #{:conn/ping :conn/pong} first)))
                             (stubs/calls stub :send!))]
              (is (empty? msgs)))))
        (testing "#on-message"
          (testing "responds to keep-alive message"
            (ps/on-message handler [:serialized [:conn/ping]])
            (let [msgs (into #{} (map first) (stubs/calls stub :send!))]
              (is (contains? msgs [:conn/pong])))))

        (testing "#on-close"
          (ps/on-close handler)
          (stubs/init! stub)
          (stubs/set-stub! stub :open? false)
          (testing "unsubscribes from topics"
            (ps/broadcast! pubsub "event-id" {:another :event})
            (ps/send-user! pubsub ::user-id "event-id" {:another :event})
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
          (is (= ::open? (ps/open? channel))))

        (testing "#send!"
          (ps/send! channel ::msg)
          (let [[[msg] :as calls] (stubs/calls stub :send!)]
            (testing "send the serialized message"
              (is (= 1 (count calls)))
              (is (= [:serialized ::msg] msg)))))

        (testing "#close!"
          (testing "when the underlying channel throws an exception"
            (stubs/set-stub! stub :close! (fn [] (throw (Exception.))))
            (testing "returns nil"
              (is (nil? (ps/close! channel))))))))))
