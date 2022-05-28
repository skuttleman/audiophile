(ns ^:integration audiophile.test.integration.events-test
  (:require
    [clojure.test :refer [are deftest is testing]]
    [audiophile.common.core.serdes.impl :as serde]
    [audiophile.common.core.utils.uuids :as uuids]
    [audiophile.common.infrastructure.http.core :as http]
    [audiophile.test.integration.common :as int]
    [audiophile.test.integration.common.http :as ihttp]))

(deftest fetch-all-events-test
  (testing "GET /api/events"
    (int/with-config [system [:api/handler]]
      (let [user (int/lookup-user system "joe@example.com")
            handler (-> system
                        (int/component :api/handler)
                        (ihttp/with-serde serde/transit))]
        (testing "when authenticated as a user with events"
          (let [result (-> {}
                           (ihttp/login system user)
                           (ihttp/get system :routes.api/events)
                           handler)]
            (testing "fetches all events"
              (is (http/success? result))
              (is (= 5 (count (get-in result [:body :data]))))

              (testing "fetches events since an id"
                (let [since (-> result :body :data second :event/id)
                      result (-> {}
                                 (ihttp/login system user)
                                 (ihttp/get system :routes.api/events {:params {:since since}})
                                 handler)]
                  (is (http/success? result))
                  (is (= 1 (count (get-in result [:body :data])))))))))

        (testing "when authenticated as a user with no events"
          (let [result (-> {}
                           (ihttp/login system {:user/id (uuids/random)})
                           (ihttp/get system :routes.api/events)
                           handler)]
            (testing "fetches no events"
              (is (http/success? result))
              (is (empty? (get-in result [:body :data]))))))

        (testing "when not authenticated"
          (let [result (-> {}
                           (ihttp/get system :routes.api/events)
                           handler)]
            (testing "fetches no events"
              (is (http/client-error? result)))))))))
