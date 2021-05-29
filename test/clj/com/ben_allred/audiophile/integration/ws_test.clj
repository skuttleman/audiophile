(ns ^:integration com.ben-allred.audiophile.integration.ws-test
  (:require
    [clojure.core.async :as async]
    [clojure.test :refer [are deftest is testing]]
    [com.ben-allred.audiophile.common.services.serdes.core :as serdes]
    [com.ben-allred.audiophile.common.utils.http :as http]
    [com.ben-allred.audiophile.common.utils.logger :as log]
    [com.ben-allred.audiophile.common.utils.uuids :as uuids]
    [com.ben-allred.audiophile.integration.common :as int]
    [com.ben-allred.audiophile.integration.common.http :as ihttp]
    [test.utils :as tu]))

(deftest ws-connection-test
  (testing "GET /api/ws"
    (int/with-config [system [:api/handler#api]]
      (let [mime-type (serdes/mime-type (int/component system :serdes/edn))
            handler (int/component system :api/handler#api)]
        (testing "when the request is authenticated"
          (ihttp/with-ws [ch (-> {}
                                 (ihttp/login system {:user/id (uuids/random)})
                                 (ihttp/get system :ws/connection {:query-params {:content-type mime-type
                                                                                  :accept       mime-type}})
                                 ihttp/as-ws
                                 handler)]
            (testing "returns a heart-beating web socket"
              (is (= [:conn/ping] (tu/<!!ms ch)))
              (async/>!! ch [:conn/ping])
              (is (= [:conn/pong] (tu/<!!ms ch))))))

        (testing "when the request is not authenticated"
          (let [response (-> {}
                             (ihttp/get system :ws/connection {:query-params {:content-type mime-type
                                                                              :accept       mime-type}})
                             ihttp/as-ws
                             handler)]
            (testing "rejects the request"
              (is (http/client-error? response)))))))))
