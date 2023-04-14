(ns ^:unit audiophile.backend.infrastructure.pubsub.ws-test
  (:require
    [audiophile.backend.api.pubsub.core :as ps]
    [audiophile.backend.api.pubsub.protocols :as pps]
    [audiophile.backend.infrastructure.pubsub.ws :as ws]
    [audiophile.common.core.utils.colls :as colls]
    [audiophile.common.core.utils.fns :as fns]
    [audiophile.common.core.utils.uuids :as uuids]
    [audiophile.test.utils :as tu]
    [audiophile.test.utils.services :as ts]
    [audiophile.test.utils.stubs :as stubs]
    [clojure.test :refer [are deftest is testing]]))

(deftest sub?-test
  (testing "(sub?)"
    (testing "[:projects ?id]"
      (let [tx (ts/->tx)
            [user-id project-id] (repeatedly uuids/random)]
        (testing "when the user has access to the project"
          (stubs/use! tx :execute! [{}])
          (is (ws/sub? {::ws/repo tx ::ws/user-id user-id} [:projects project-id]))
          (let [[{:keys [from where]}] (colls/only! (stubs/calls tx :execute!))]
            (is (= [:projects] from))
            (is (= [:and
                    #{[:= #{:projects.id project-id}]
                      [:exists
                       {:from   [:user-teams]
                        :select #{1}
                        :where  [:and
                                 #{[:= #{:projects.team-id :user-teams.team-id}]
                                   [:= #{:user-teams.user-id user-id}]}]}]}]
                   (-> where
                       (update 1 tu/op-set)
                       (update-in [2 1] (fns/=> (update :select set)
                                                (update :where (fns/=> (update 1 tu/op-set)
                                                                       (update 2 tu/op-set)
                                                                       tu/op-set))))
                       tu/op-set)))))

        (testing "when the user does not have access to the project"
          (stubs/use! tx :execute! [])
          (is (not (ws/sub? {::ws/repo tx ::ws/user-id user-id} [:projects project-id]))))))

    (testing "[:teams ?id]"
      (let [tx (ts/->tx)
            [user-id team-id] (repeatedly uuids/random)]
        (testing "when the user has access to the team"
          (stubs/use! tx :execute! [{}])
          (is (ws/sub? {::ws/repo tx ::ws/user-id user-id} [:teams team-id]))
          (let [[{:keys [from where]}] (colls/only! (stubs/calls tx :execute!))]
            (is (= [:teams] from))
            (is (= [:and
                    #{[:= #{:teams.id team-id}]
                      [:exists
                       {:from   [:user-teams]
                        :select #{1}
                        :where  [:and
                                 #{[:= #{:teams.id :user-teams.team-id}]
                                   [:= #{:user-teams.user-id user-id}]}]}]}]
                   (-> where
                       (update 1 tu/op-set)
                       (update-in [2 1] (fns/=> (update :select set)
                                                (update :where (fns/=> (update 1 tu/op-set)
                                                                       (update 2 tu/op-set)
                                                                       tu/op-set))))
                       tu/op-set)))))

        (testing "when the user does not have access to the team"
          (stubs/use! tx :execute! [])
          (is (not (ws/sub? {::ws/repo tx ::ws/user-id user-id} [:teams team-id]))))))))

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
          ctx {::ws/ch-id  ch-id
               ::ws/pubsub pubsub}]
      (testing "when the channel is closed"
        (ws/on-close! ctx ch)

        (testing "unsubscribes from all topics"
          (is (= ch-id (-> pubsub
                           (stubs/calls :unsubscribe!)
                           colls/only!
                           colls/only!))))))))

(deftest event-handler-test
  (let [pubsub (ts/->pubsub)
        handler (ws/event-handler {:pubsub pubsub})
        [event-id request-id some-id user-id workflow-id] (repeatedly uuids/random)]
    (testing "when emitting :workflow/completed event"
      (stubs/init! pubsub)
      (handler {:value {:event/id   event-id
                        :event/type :workflow/completed
                        :event/data {:scope              {'?foo some-id}
                                     :workflows/->result '{:foo/id (spigot/get ?foo)}
                                     :workflows/template :foo/bar}
                        :event/ctx  {:user/id     user-id
                                     :request/id  request-id
                                     :workflow/id workflow-id}}})
      (testing "publishes a user event"
        (let [[topic [_ event ctx]] (colls/only! (stubs/calls pubsub :publish!))]
          (is (= [::ps/user user-id] topic))
          (is (= {:event/id   event-id
                  :event/type :workflow/completed
                  :event/data {:workflow/id       workflow-id
                               :workflow/template :foo/bar
                               :foo/id            some-id}}
                 event))
          (is (= {:user/id     user-id
                  :request/id  request-id
                  :workflow/id workflow-id}
                 ctx)))))

    (testing "when emitting :workflow/failed event"
      (stubs/init! pubsub)
      (handler {:value {:event/id   event-id
                        :event/type :workflow/failed
                        :event/data {:some :error}
                        :event/ctx  {:user/id     user-id
                                     :request/id  request-id
                                     :workflow/id workflow-id}}})
      (testing "publishes a user event"
        (let [[topic [_ event ctx]] (colls/only! (stubs/calls pubsub :publish!))]
          (is (= [::ps/user user-id] topic))
          (is (= {:event/id   event-id
                  :event/type :workflow/failed
                  :event/data {:some :error}}
                 event))
          (is (= {:user/id     user-id
                  :request/id  request-id
                  :workflow/id workflow-id}
                 ctx)))))))
