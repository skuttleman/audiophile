(ns ^:integration audiophile.test.integration.ws-test
  (:require
    [audiophile.backend.api.pubsub.core :as ps]
    [audiophile.common.core.serdes.core :as serdes]
    [audiophile.common.core.serdes.impl :as serde]
    [audiophile.common.core.utils.core :as u]
    [audiophile.common.core.utils.logger :as log]
    [audiophile.common.core.utils.uuids :as uuids]
    [audiophile.common.infrastructure.http.core :as http]
    [audiophile.test.integration.common :as int]
    [audiophile.test.integration.common.http :as ihttp]
    [audiophile.test.utils :as tu]
    [clojure.core.async :as async]
    [clojure.test :refer [are deftest is testing]]))

(deftest ws-connection-test
  (testing "GET /api/ws"
    (int/with-config [system [:api/handler]]
      (let [mime-type (serdes/mime-type serde/edn)
            handler (int/component system :api/handler)]
        (testing "when the request is authenticated"
          (ihttp/with-ws [ch (-> {}
                                 (ihttp/login system {:user/id (uuids/random)})
                                 (ihttp/get system
                                            :routes.ws/connection
                                            {:params {:content-type mime-type
                                                      :accept       mime-type}})
                                 ihttp/as-ws
                                 handler)]
            (testing "returns a heart-beating web socket"
              (is (= [:conn/ping] (tu/<!!ms ch)))
              (async/>!! ch [:conn/ping])
              (is (= [:conn/pong] (tu/<!!ms ch))))))

        (testing "when the request is not authenticated"
          (let [response (-> {}
                             (ihttp/get system
                                        :routes.ws/connection
                                        {:params {:content-type mime-type
                                                  :accept       mime-type}})
                             ihttp/as-ws
                             handler)]
            (testing "rejects the request"
              (is (http/client-error? response)))))))))

(deftest ws-subscription-test
  (testing "GET /api/ws"
    (int/with-config [system [:api/handler :services/pubsub]]
      (let [user (int/lookup-user system "joe@example.com")
            project-id (:project/id (int/lookup-project system "Project Seed"))
            mime-type (serdes/mime-type serde/edn)
            handler (int/component system :api/handler)
            pubsub (int/component system :services/pubsub)]
        (testing "when the request is authenticated"
          (ihttp/with-ws [ch (-> {}
                                 (ihttp/login system user)
                                 (ihttp/get system
                                            :routes.ws/connection
                                            {:params {:content-type mime-type
                                                      :accept       mime-type}})
                                 (ihttp/as-ws {:no-keep-alive? true})
                                 handler)]
            (testing "and when subscribing to an allowed topic"
              (async/>!! ch [:sub/start! [:projects project-id]])
              (testing "and when publishing an event"
                (ps/publish! pubsub [:projects project-id] "msg-id" {:some :msg} {:sub/id [:projects project-id]})
                (testing "receives the event from the websocket"
                  (is (= [:event/subscription "msg-id" {:some :msg} {:sub/id [:projects project-id]}]
                         (tu/<!!ms ch))))))

            (testing "and when subscribing to a disallowed topic"
              (let [no-project-id (uuids/random)]
                (async/>!! ch [:sub/start! [:projects no-project-id]])
                (testing "and when publishing an event"
                  (ps/publish! pubsub [:projects no-project-id] "msg-id" {:some :msg} {:sub/id [:projects no-project-id]})
                  (testing "does not receives the event from the websocket"
                    (is (thrown? Throwable (tu/<!!ms ch 500)))))))))))))
