(ns ^:unit com.ben-allred.audiophile.ui.infrastructure.pubsub.ws-test
  (:require
    [clojure.test :refer [deftest is testing]]
    [com.ben-allred.audiophile.common.app.navigation.base :as bnav]
    [com.ben-allred.audiophile.common.core.serdes.protocols :as pserdes]
    [com.ben-allred.audiophile.ui.infrastructure.pubsub.ws :as ws]
    [com.ben-allred.audiophile.ui.infrastructure.store.protocols :as pstore]
    [test.utils.stubs :as stubs]))

(deftest handle-msg-test
  (testing "handle-msg"
    (let [store (stubs/create (reify
                                pstore/IStore
                                (dispatch! [_ _])))]
      (testing "handles default message"
        (ws/handle-msg store [::msg-type ::event-id ::data])
        (let [[event] (peek (stubs/calls store :dispatch!))]
          (is (= [:ws/message [::msg-type {:id   ::event-id
                                           :data ::data}]]
                 event))))

      (testing "handles contextual message"
        (ws/handle-msg store [::msg-type ::event-id ::data ::ctx])
        (let [[event] (peek (stubs/calls store :dispatch!))]
          (is (= [:ws/message [::msg-type {:id   ::event-id
                                           :data ::data
                                           :ctx  ::ctx}]]
                 event))))

      (testing "ignores other messages"
        (stubs/init! store)
        (ws/handle-msg store [:foo "here"])
        (ws/handle-msg store :bar)
        (ws/handle-msg store 13)
        (ws/handle-msg store {:another :thing})

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
