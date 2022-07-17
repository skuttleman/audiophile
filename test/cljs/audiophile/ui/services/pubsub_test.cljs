(ns ^:unit audiophile.ui.services.pubsub-test
  (:require
    [audiophile.common.api.pubsub.core :as pubsub]
    [audiophile.common.api.pubsub.protocols :as ppubsub]
    [audiophile.common.core.utils.colls :as colls]
    [audiophile.common.infrastructure.protocols :as pcom]
    [audiophile.test.utils :refer [async] :as tu]
    [audiophile.test.utils.spies :as spies]
    [audiophile.test.utils.stubs :as stubs]
    [audiophile.ui.services.pages :as pages]
    [audiophile.ui.services.pubsub :as pubsub.ui]
    [clojure.core.async :as async]
    [clojure.core.async.impl.protocols :as async.protocols]
    [clojure.test :refer [are deftest is testing]]))

(deftest ws-pubsub-test
  (testing "WsPubSub"
    (let [ps-stub (stubs/create (reify
                                  ppubsub/IPub
                                  (publish! [_ _ _])
                                  ppubsub/ISub
                                  (subscribe! [_ _ _ _])
                                  (unsubscribe! [_ _ _])))
          ch (async/chan 10)
          ws-spy (spies/create ch)
          subs (atom #{[:topic 1] [:topic 2]})
          pubsub (pubsub.ui/->WsPubSub ps-stub ws-spy subs nil)]
      (async done
        (async/go
          (testing "cannot subscribe to uninitialized pubsub"
            (is (thrown? js/Object (pubsub/subscribe! pubsub ::pages/sub [:some :topic] ::listener))))

          (testing "#init!"
            (pcom/init! pubsub)
            (testing "initializes the websocket"
              (let [[{:keys [on-connect pubsub]}] (colls/only! (spies/calls ws-spy))]
                (is (= pubsub pubsub))
                (is (fn? on-connect))

                (tu/<ch! (on-connect ch))
                (let [msg (tu/<ch! ch)
                      msgs (set [(tu/<ch! ch) (tu/<ch! ch)])]
                  (is (= [:conn/ping] msg))
                  (is (contains? msgs [:sub/start! [:topic 1]]))
                  (is (contains? msgs [:sub/start! [:topic 2]]))))))

          (testing "#publish!"
            (pubsub/publish! pubsub [:some :topic] {:some :msg})
            (testing "publishes to underlying pubsub"
              (let [call (colls/only! (stubs/calls ps-stub :publish!))]
                (is (= [[:some :topic] {:some :msg}]
                       call)))))

          (testing "#subscribe!"
            (pubsub/subscribe! pubsub ::pages/sub [:topic 3] ::listener)
            (testing "adds the subscription"
              (is (contains? @subs [:topic 3])))

            (testing "subscribes to underlying pubsub"
              (let [[key topic] (colls/only! (stubs/calls ps-stub :subscribe!))]
                (is (= ::pages/sub key))
                (is (= [:topic 3] topic))))

            (testing "sends subscription via websocket"
              (let [msg (tu/<ch! ch)]
                (is (= [:sub/start! [:topic 3]] msg)))))

          (testing "#unsubscribe!"
            (pubsub/unsubscribe! pubsub ::pages/sub [:topic 3])
            (testing "removes the subscription"
              (is (not (contains? @subs [:topic 3]))))

            (testing "unsubscribes from underlying pubsub"
              (let [[key topic] (colls/only! (stubs/calls ps-stub :unsubscribe!))]
                (is (= ::pages/sub key))
                (is (= [:topic 3] topic))))

            (testing "sends subscription removal via websocket"
              (let [msg (tu/<ch! ch)]
                (is (= [:sub/stop! [:topic 3]] msg)))))

          (testing "#destroy!"
            (pcom/destroy! pubsub)
            (testing "stops subscriptions"
              (let [msgs (set [(tu/<ch! ch) (tu/<ch! ch)])]
                (is (contains? msgs [:sub/stop! [:topic 1]]))
                (is (contains? msgs [:sub/stop! [:topic 2]]))))

            (testing "closes websocket"
              (async.protocols/closed? ch))

            (testing "when creating a page subscription"
              (testing "cannot subscribe to uninitialized pubsub"
                (is (thrown? js/Object (pubsub/subscribe! pubsub ::pages/sub [:some :topic] ::listener)))))

            (testing "when creating a non-page subscription"
              (testing "subscribes to the topic"
                (is (pubsub/subscribe! pubsub ::key [:some :topic] ::listener)))))

          (done))))))
