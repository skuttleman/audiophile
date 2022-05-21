(ns ^:unit audiophile.backend.infrastructure.pubsub.ws-test
  (:require
    [clojure.test :refer [are deftest is testing]]
    [audiophile.backend.api.pubsub.core :as ps]
    [audiophile.backend.api.pubsub.protocols :as pps]
    [audiophile.backend.infrastructure.pubsub.ws :as ws]
    [audiophile.common.core.utils.colls :as colls]
    [audiophile.common.core.utils.uuids :as uuids]
    [audiophile.test.utils.services :as ts]
    [audiophile.test.utils.stubs :as stubs]))

(deftest on-message!-test
  (testing "(on-message!)"
    (let [ch (ts/->chan)]
      (testing "when receiving a ping"
        (ws/on-message! {} ch [:conn/ping])

        (testing "responds with a pong"
          (is (= [:conn/pong] (-> ch
                                  (stubs/calls :send!)
                                  colls/only!
                                  first))))))))

(deftest on-open!-test
  (testing "(on-open)"
    (let [ch (stubs/create (reify
                             pps/IChannel
                             (send! [_ _])
                             (open? [_] true)))]
      (try
        (let [pubsub (ts/->pubsub)
              [ch-id user-id] (repeatedly uuids/random)
              ctx {::ws/heartbeat-int-ms 50
                   ::ws/ch-id            ch-id
                   ::ws/user-id          user-id
                   ::ws/pubsub           pubsub}]
          (testing "when a web socket is opened"
            (stubs/set-stub! ch :open? true)
            (ws/on-open! ctx ch)

            (let [subs (into {}
                             (map (juxt second identity))
                             (stubs/calls pubsub :subscribe!))]
              (is (= 2 (count subs)))

              (let [[ch-id* topic* f*] (get subs [::ps/user user-id])]
                (testing "subscribes to the user's topic"
                  (is (= ch-id ch-id*))
                  (is (= topic* [::ps/user user-id])))

                (testing "and when a message is published to the user's topic"
                  (stubs/init! ch)
                  (f* ch-id [:some :event])

                  (testing "sends the message via websocket"
                    (let [msg (->> (stubs/calls ch :send!)
                                   (map colls/only!)
                                   (remove #{[:conn/ping]})
                                   colls/only!)]
                      (is (= [:event/user :some :event]
                             msg))))))

              (let [[ch-id* topic* f*] (get subs [::ps/broadcast])]
                (testing "subscribes to the broadcast topic"
                  (is (= ch-id ch-id*))
                  (is (= topic* [::ps/broadcast])))

                (testing "and when a message is published to the broadcast topic"
                  (stubs/init! ch)
                  (f* ch-id [:some :event])

                  (testing "sends the message via websocket"
                    (let [msg (->> (stubs/calls ch :send!)
                                   (map colls/only!)
                                   (remove #{[:conn/ping]})
                                   colls/only!)]
                      (is (= [:event/broadcast :some :event]
                             msg))))))

              (testing "pings the connection"
                (Thread/sleep 100)
                (is (seq (->> (stubs/calls ch :send!)
                              (filter #{[[:conn/ping]]}))))))))
        (finally
          (stubs/set-stub! ch :open? false))))))

(deftest on-close!-test
  (testing "(on-close!)"
    (let [ch (ts/->chan)
          pubsub (ts/->pubsub)
          ch-id (uuids/random)
          ctx {::ws/ch-id ch-id
               ::ws/pubsub pubsub}]
      (testing "when the channel is closed"
        (ws/on-close! ctx ch)

        (testing "unsubscribes from all topics"
          (is (= ch-id (-> pubsub
                           (stubs/calls :unsubscribe!)
                           colls/only!
                           colls/only!))))))))
