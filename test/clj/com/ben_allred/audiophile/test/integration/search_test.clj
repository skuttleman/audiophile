(ns ^:integration com.ben-allred.audiophile.test.integration.search-test
  (:require
    [clojure.test :refer [are deftest is testing]]
    [com.ben-allred.audiophile.common.core.utils.uuids :as uuids]
    [com.ben-allred.audiophile.common.infrastructure.http.core :as http]
    [com.ben-allred.audiophile.test.integration.common :as int]
    [com.ben-allred.audiophile.test.integration.common.http :as ihttp]))

(deftest search-handle-test
  (testing "GET /api/search/:entity/:name/:value"
    (int/with-config [system [:api/handler]]
      (let [user (int/lookup-user system "joe@example.com")
            handler (-> system
                        (int/component :api/handler)
                        (ihttp/with-serde system :serdes/edn))]
        (testing "when the handle is in use"
          (let [response (-> {}
                             (ihttp/login system {:user/id (uuids/random)} {:jwt/claims {:aud #{:token/signup}}})
                             (ihttp/get system :api/search {:params {:field/entity "user"
                                                                     :field/name   "handle"
                                                                     :field/value  (:user/handle user)}})
                             handler)]
            (testing "returns true"
              (is (http/success? response))
              (is (= {:in-use? true} (get-in response [:body :data]))))))

        (testing "when the handle is not in use"
          (let [response (-> {}
                             (ihttp/login system {:user/id (uuids/random)} {:jwt/claims {:aud #{:token/signup}}})
                             (ihttp/get system :api/search {:params {:field/entity "user"
                                                                     :field/name   "handle"
                                                                     :field/value  "unused-handle"}})
                             handler)]
            (testing "returns false"
              (is (http/success? response))
              (is (= {:in-use? false} (get-in response [:body :data]))))))

        (testing "when not authenticated"
          (let [response (-> {}
                             (ihttp/get system :api/search {:params {:field/entity "user"
                                                                     :field/name   "mobile-number"
                                                                     :field/value  (:user/handle user)}})
                             handler)]
            (testing "returns an error"
              (is (http/client-error? response)))))))))

(deftest search-mobile-number-test
  (testing "GET /api/search/:entity/:name/:value"
    (int/with-config [system [:api/handler]]
      (let [user (int/lookup-user system "joe@example.com")
            handler (-> system
                        (int/component :api/handler)
                        (ihttp/with-serde system :serdes/edn))]
        (testing "when the mobile-number is in use"
          (let [response (-> {}
                             (ihttp/login system {:user/id (uuids/random)} {:jwt/claims {:aud #{:token/signup}}})
                             (ihttp/get system :api/search {:params {:field/entity "user"
                                                                     :field/name   "mobile-number"
                                                                     :field/value  (:user/mobile-number user)}})
                             handler)]
            (testing "returns true"
              (is (http/success? response))
              (is (= {:in-use? true} (get-in response [:body :data]))))))

        (testing "when the mobile-number is not in use"
          (let [response (-> {}
                             (ihttp/login system {:user/id (uuids/random)} {:jwt/claims {:aud #{:token/signup}}})
                             (ihttp/get system :api/search {:params {:field/entity "user"
                                                                     :field/name   "mobile-number"
                                                                     :field/value  "9019019019"}})
                             handler)]
            (testing "returns false"
              (is (http/success? response))
              (is (= {:in-use? false} (get-in response [:body :data]))))))

        (testing "when not authenticated"
          (let [response (-> {}
                             (ihttp/get system :api/search {:params {:field/entity "user"
                                                                     :field/name   "mobile-number"
                                                                     :field/value (:user/mobile-number user)}})
                             handler)]
            (testing "returns an error"
              (is (http/client-error? response)))))))))
