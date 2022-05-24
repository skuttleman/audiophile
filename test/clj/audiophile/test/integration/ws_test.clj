(ns ^:integration audiophile.test.integration.ws-test
  (:require
    [clojure.core.async :as async]
    [clojure.test :refer [are deftest is testing]]
    [audiophile.common.core.serdes.core :as serdes]
    [audiophile.common.core.serdes.impl :as serde]
    [audiophile.common.core.utils.logger :as log]
    [audiophile.common.core.utils.uuids :as uuids]
    [audiophile.common.infrastructure.http.core :as http]
    [audiophile.test.integration.common :as int]
    [audiophile.test.integration.common.http :as ihttp]
    [audiophile.test.utils :as tu]))

(deftest ws-connection-test
  (testing "GET /api/ws"
    (int/with-config [system [:api/handler]]
      (let [mime-type (serdes/mime-type serde/edn)
            handler (int/component system :api/handler)]
        (testing "when the request is authenticated"
          (ihttp/with-ws [ch (-> {}
                                 (ihttp/login system {:user/id (uuids/random)})
                                 (ihttp/get system
                                            :ws/connection
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
                                        :ws/connection
                                        {:params {:content-type mime-type
                                                  :accept       mime-type}})
                             ihttp/as-ws
                             handler)]
            (testing "rejects the request"
              (is (http/client-error? response)))))))))
