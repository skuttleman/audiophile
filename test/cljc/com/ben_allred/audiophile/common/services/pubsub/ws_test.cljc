(ns ^:unit com.ben-allred.audiophile.common.services.pubsub.ws-test
  (:require
    [clojure.test :refer [deftest is testing]]
    [com.ben-allred.audiophile.common.services.pubsub.ws :as ws]
    [com.ben-allred.audiophile.common.services.ui-store.protocols :as pui-store]
    [test.utils.mocks :as mocks]
    [integrant.core :as ig]
    [com.ben-allred.audiophile.common.services.navigation.core :as nav]
    [com.ben-allred.audiophile.common.services.serdes.protocols :as pserdes]))

(deftest handle-msg-test
  (testing "handle-msg"
    (let [store (mocks/->mock (reify
                                pui-store/IStore
                                (dispatch! [_ _])))]
      (testing "handles default message"
        (ws/handle-msg store [::msg-type ::event-id ::data])
        (let [[event] (peek (mocks/calls store :dispatch!))]
          (is (= [:ws/message [::msg-type {:id   ::event-id
                                           :data ::data}]]
                 event))))

      (testing "handles contextual message"
        (ws/handle-msg store [::msg-type ::event-id ::data ::ctx])
        (let [[event] (peek (mocks/calls store :dispatch!))]
          (is (= [:ws/message [::msg-type {:id   ::event-id
                                           :data ::data
                                           :ctx  ::ctx}]]
                 event))))

      (testing "ignores other messages"
        (mocks/init! store)
        (ws/handle-msg store [:foo "here"])
        (ws/handle-msg store :bar)
        (ws/handle-msg store 13)
        (ws/handle-msg store {:another :thing})

        (is (empty? (mocks/calls store :dispatch!)))))))

(deftest ws-uri-test
  (testing "ws-uri"
    (let [base-url "https://uri.base"
          serde (reify
                  pserdes/ISerde
                  (mime-type [_]
                    "charlie/chaplain")
                  (serialize [_ value opts]
                    (str "/" value "?content-type=" (get-in opts [:query-params :content-type]))))
          nav (nav/->LinkedNavigator nil serde)]
      (is (= "wss://uri.base/:api/ws?content-type=charlie/chaplain"
             (ws/ws-uri nav serde base-url))))))
