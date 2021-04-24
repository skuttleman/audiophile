(ns ^:unit com.ben-allred.audiophile.api.services.pubsub.ws-test
  (:require
    [clojure.test :refer [are deftest is testing]]
    [com.ben-allred.audiophile.api.services.pubsub.ws :as ws]
    [com.ben-allred.audiophile.common.services.pubsub.core :as pubsub]
    [com.ben-allred.audiophile.common.services.serdes.protocols :as pserdes]
    [com.ben-allred.audiophile.common.utils.logger :as log]
    [integrant.core :as ig]
    [test.utils.mocks :as mocks]))

(deftest ->handler-test
  (let [pubsub (ig/init-key ::pubsub/pubsub {})
        ->handler (ig/init-key ::ws/->handler
                               {:heartbeat-int-ms 100
                                :pubsub           pubsub
                                :serdes           {:foo/bar (reify pserdes/ISerde
                                                              (mime-type [_]
                                                                "foo/bar")
                                                              (serialize [_ value _]
                                                                [:serialized value])
                                                              (deserialize [_ value _]
                                                                (second value)))}})
        request {:auth/user {:data {:user {:user/id ::user-id}}}
                 :nav/route {:query-params {:accept "foo/bar"}}}
        mock (mocks/->mock (reify ws/IChannel
                             (open? [_] ::open?)
                             (send! [_ _])
                             (close! [_] ::close!)))]
    (testing "->handler"
      (let [handler (->handler request mock)]
        (testing "#on-open"
          (ws/on-open handler)
          (Thread/sleep 300)
          (testing "establishes a heartbeat"
            (let [msgs (frequencies (map first (mocks/calls mock :send!)))]
              (is (<= 2 (get msgs [:conn/ping])))))

          (testing "subscribes to broadcasts"
            (ws/broadcast! pubsub "event-id" {:some :event})
            (let [msgs (into #{}
                             (comp (map first)
                                   (remove (comp #{:conn/ping :conn/pong} first)))
                             (mocks/calls mock :send!))]
              (is (contains? msgs [:event/broadcast "event-id" {:some :event}]))))

          (testing "subscribes to user-level messages"
            (ws/send-user! pubsub ::user-id "event-id" {:some :event})
            (let [msgs (into #{}
                             (comp (map first)
                                   (remove (comp #{:conn/ping :conn/pong} first)))
                             (mocks/calls mock :send!))]
              (is (contains? msgs [:event/user "event-id" {:some :event} {:user/id ::user-id}]))))

          (testing "is not subscribed to other topics"
            (mocks/init! mock)
            (ws/send-user! pubsub ::unknown "event-id" {:some :event})
            (let [msgs (into #{}
                             (comp (map first)
                                   (remove (comp #{:conn/ping :conn/pong} first)))
                             (mocks/calls mock :send!))]
              (is (empty? msgs)))))
        (testing "#on-message"
          (testing "responds to keep-alive message"
            (ws/on-message handler [:serialized [:conn/ping]])
            (let [msgs (into #{} (map first) (mocks/calls mock :send!))]
              (is (contains? msgs [:conn/pong])))))

        (testing "#on-close"
          (ws/on-close handler)
          (mocks/init! mock)
          (mocks/set-mock! mock :open? false)
          (testing "unsubscribes from topics"
            (ws/broadcast! pubsub "event-id" {:another :event})
            (ws/send-user! pubsub ::user-id "event-id" {:another :event})
            (is (empty? (mocks/calls mock :send!))))

          (testing "stops sending heartbeats"
            (Thread/sleep 300)
            (is (empty? (mocks/calls mock :send!)))))))))

(deftest ->channel-test
  (let [->channel (ig/init-key ::ws/->channel
                               {:serdes {:foo/bar (reify pserdes/ISerde
                                                    (mime-type [_]
                                                      "foo/bar")
                                                    (serialize [_ value _]
                                                      [:serialized value])
                                                    (deserialize [_ value _]
                                                      [:deserialized value]))}})
        request {:nav/route {:query-params {:accept "foo/bar"}}}
        mock (mocks/->mock (reify ws/IChannel
                             (open? [_] ::open?)
                             (send! [_ _])
                             (close! [_] ::close!)))]
    (testing "->channel"
      (let [channel (->channel request mock)]
        (testing "#open?"
          (is (= ::open? (ws/open? channel))))

        (testing "#send!"
          (ws/send! channel ::msg)
          (let [[[msg] :as calls] (mocks/calls mock :send!)]
            (testing "send the serialized message"
              (is (= 1 (count calls)))
              (is (= [:serialized ::msg] msg)))

            (testing "when the underlying channel throws an exception"
              (mocks/set-mock! mock :send! (fn [_] (throw (Exception.))))
              (testing "returns nil"
                (is (nil? (ws/send! channel ::msg)))))))

        (testing "#close!"
          (is (= ::close! (ws/close! channel)))

          (testing "when the underlying channel throws an exception"
            (mocks/set-mock! mock :close! (fn [] (throw (Exception.))))
            (testing "returns nil"
              (is (nil? (ws/close! channel))))))))))
