(ns com.ben-allred.audiophile.integration.events-test
  (:require
    [clojure.test :refer [are deftest is testing]]
    [com.ben-allred.audiophile.common.core.utils.uuids :as uuids]
    [com.ben-allred.audiophile.common.infrastructure.http.core :as http]
    [com.ben-allred.audiophile.integration.common :as int]
    [com.ben-allred.audiophile.integration.common.http :as ihttp]))

(deftest fetch-all-events-test
  (testing "GET /api/events"
    (int/with-config [system [:api/handler#api]] {:db/enabled? true}
      (let [user (int/lookup-user system "joe@example.com")
            handler (-> system
                        (int/component :api/handler#api)
                        (ihttp/with-serde system :serdes/edn))]
        (testing "when authenticated as a user with events"
          (let [result (-> {}
                           (ihttp/login system user)
                           (ihttp/get system :api/events)
                           handler)]
            (testing "fetches all events"
              (is (http/success? result))
              (is (= 3 (count (get-in result [:body :data]))))

              (testing "fetches events since an id"
                (let [since (-> result :body :data second :event/id)
                      result (-> {}
                                 (ihttp/login system user)
                                 (ihttp/get system :api/events {:query-params {:since since}})
                                 handler)]
                  (is (http/success? result))
                  (is (= 1 (count (get-in result [:body :data])))))))))

        (testing "when authenticated as a user with no events"
          (let [result (-> {}
                           (ihttp/login system {:user/id (uuids/random)})
                           (ihttp/get system :api/events)
                           handler)]
            (testing "fetches no events"
              (is (http/success? result))
              (is (empty? (get-in result [:body :data]))))))

        (testing "when not authenticated"
          (let [result (-> {}
                           (ihttp/get system :api/events)
                           handler)]
            (testing "fetches no events"
              (is (http/client-error? result)))))))))
