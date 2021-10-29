(ns ^:integration com.ben-allred.audiophile.test.integration.users-test
  (:require
    [clojure.test :refer [are deftest is testing use-fixtures]]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.common.core.utils.uuids :as uuids]
    [com.ben-allred.audiophile.common.infrastructure.http.core :as http]
    [com.ben-allred.audiophile.test.integration.common :as int]
    [com.ben-allred.audiophile.test.integration.common.http :as ihttp]))

(deftest user-profile-test
  (testing "GET /users/profile"
    (int/with-config [system [:api/handler]] {:db/enabled? true}
      (let [user (int/lookup-user system "joe@example.com")
            handler (-> system
                        (int/component :api/handler)
                        (ihttp/with-serde system :serdes/edn))]
        (testing "when the profile is found"
          (let [response (-> {}
                             (ihttp/login system user)
                             (ihttp/get system :api/profile)
                             handler)]
            (testing "returns the profile"
              (is (http/success? response))
              (is (= user
                     (-> response
                         (get-in [:body :data])
                         (select-keys (keys user))))))))

        (testing "when the auth provider interactions fail"
          (let [response (-> {}
                             (ihttp/login system {:user/id (uuids/random)})
                             (ihttp/get system :api/profile)
                             handler)]
            (testing "returns an error"
              (is (http/client-error? response)))))))))
