(ns ^:unit com.ben-allred.audiophile.ui.infrastructure.pubsub.ws-test
  (:require
    [clojure.test :refer [deftest is testing]]
    [com.ben-allred.audiophile.common.api.navigation.base :as bnav]
    [com.ben-allred.audiophile.common.core.serdes.protocols :as pserdes]
    [com.ben-allred.audiophile.common.infrastructure.pubsub.protocols :as ppubsub]
    [com.ben-allred.audiophile.ui.infrastructure.pubsub.ws :as ws]
    [com.ben-allred.audiophile.ui.infrastructure.store.protocols :as pstore]
    [test.utils.stubs :as stubs]))

(deftest handle-msg-test
  (testing "handle-msg"
    (let [store (stubs/create (reify
                                pstore/IStore
                                (dispatch! [_ _])))
          pubsub (stubs/create (reify
                                 ppubsub/IPub
                                 (publish! [_ _ _])))]
      (testing "handles contextual message"
        (ws/handle-msg pubsub store [::msg-type ::event-id {:event/data ::data} {:some :ctx :request/id "request-id"}])
        (let [[action] (peek (stubs/calls store :dispatch!))
              [topic event] (peek (stubs/calls pubsub :publish!))]
          (is (= [:ws/message [::msg-type {:id   ::event-id
                                           :data {:event/data ::data}
                                           :ctx  {:some :ctx :request/id "request-id"}}]]
                 action))
          (is (= "request-id" topic))
          (is (= {:data ::data} event))))

      (testing "ignores other messages"
        (stubs/init! pubsub)
        (stubs/init! store)
        (ws/handle-msg pubsub store [:foo "here"])
        (ws/handle-msg pubsub store :bar)
        (ws/handle-msg pubsub store 13)
        (ws/handle-msg pubsub store {:another :thing})

        (is (empty? (stubs/calls pubsub :publish!)))
        (is (empty? (stubs/calls store :dispatch!)))))))

(deftest ws-uri-test
  (testing "ws-uri"
    (let [base-url "https://uri.base"
          serde (reify
                  pserdes/ISerde
                  (serialize [_ value opts]
                    (str "/" value "?content-type=" (get-in opts [:query-params :content-type])))

                  pserdes/IMime
                  (mime-type [_]
                    "charlie/chaplain"))
          nav (bnav/->LinkedNavigator nil serde)]
      (is (= "wss://uri.base/:ws/connection?content-type=charlie/chaplain"
             (ws/ws-uri nav serde base-url))))))
