(ns ^:integration com.ben-allred.audiophile.integration.auth-test
  (:require
    [clojure.string :as string]
    [clojure.test :refer [are deftest is testing use-fixtures]]
    [com.ben-allred.audiophile.api.handlers.core :as handlers]
    [com.ben-allred.audiophile.api.utils.ring :as ring]
    [com.ben-allred.audiophile.common.services.serdes.core :as serdes]
    [com.ben-allred.audiophile.common.utils.http :as http]
    [com.ben-allred.audiophile.common.utils.logger :as log]
    [com.ben-allred.audiophile.common.utils.uri :as uri]
    [com.ben-allred.audiophile.integration.common :as int]
    [com.ben-allred.audiophile.integration.common.http :as ihttp]
    [test.utils.stubs :as stubs]))

(deftest auth-details-test
  (testing "GET /auth/details"
    (int/with-config [system [::handlers/app]]
      (let [user (int/seed-user system "joe@example.com")
            handler (-> (::handlers/app system)
                        (ihttp/with-serde system :serdes/edn))
            response (-> {}
                         (ihttp/login system user)
                         (ihttp/get system :auth/details)
                         handler)]
        (testing "returns the user details"
          (is (http/success? response))
          (is (= {:data user}
                 (update (:body response) :data dissoc :user/created-at))))))))

(deftest auth-callback-test
  (testing "GET /auth/callback"
    (int/with-config [system [::handlers/app]] {:db/enabled? true}
      (let [user (int/seed-user system "joe@example.com")
            handler (-> (::handlers/app system)
                        (ihttp/with-serde system :serdes/edn))]
        (stubs/set-stub! (int/component system :services/oauth)
                         :-profile
                         (fn [opts]
                           (if (= "secret-pin-12345" (:code opts))
                             {:email (:user/email user)}
                             (throw (ex-info "fail" {})))))
        (testing "when the auth provider interactions succeed"
          (let [handler (-> (::handlers/app system)
                            (ihttp/with-serde system :serdes/edn))
                response (-> {}
                             (ihttp/get system :auth/callback {:query-params {:code "secret-pin-12345"}})
                             handler)
                base-url (int/component system :env/base-url)
                jwt-serde (int/component system :serdes/jwt)
                cookies (ring/decode-cookies response)]
            (testing "redirects with token cookie"
              (is (http/redirect? response))
              (is (= (str base-url "/")
                     (get-in response [:headers "Location"])))
              (is (= (dissoc user :user/mobile-number :user/created-at)
                     (-> jwt-serde
                         (serdes/deserialize (get-in cookies ["auth-token" :value]))
                         :data
                         (dissoc :user/created-at)))))))

        (testing "when the auth provider interactions fail"
          (-> system
              (int/component :services/oauth)
              (stubs/set-stub! :-token nil))
          (testing "redirects with token cookie"
            (let [response (-> {}
                               (ihttp/get system :auth/callback {:query-params {:code "bad-pin"}})
                               handler)
                  base-url (int/component system :env/base-url)
                  cookies (ring/decode-cookies response)
                  location (get-in response [:headers "Location"])]
              (is (http/redirect? response))
              (is (string/starts-with? location base-url))
              (is (= "login-failed" (get-in (uri/parse location) [:query :error-msg])))
              (is (= "" (get-in cookies ["auth-token" :value]))))))))))
