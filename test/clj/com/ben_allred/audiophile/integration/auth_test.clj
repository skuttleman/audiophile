(ns ^:integration com.ben-allred.audiophile.integration.auth-test
  (:require
    [clojure.string :as string]
    [clojure.test :refer [are deftest is testing use-fixtures]]
    [com.ben-allred.audiophile.common.core.serdes.core :as serdes]
    [com.ben-allred.audiophile.common.infrastructure.http.core :as http]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.common.core.utils.uri :as uri]
    [com.ben-allred.audiophile.integration.common :as int]
    [com.ben-allred.audiophile.integration.common.http :as ihttp]
    [test.utils :as tu]
    [test.utils.stubs :as stubs]))

(deftest auth-details-test
  (testing "GET /auth/details"
    (int/with-config [system [:api/handler#auth]]
      (let [user (int/lookup-user system "joe@example.com")
            handler (-> system
                        (int/component :api/handler#auth)
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
    (int/with-config [system [:api/handler#auth]] {:db/enabled? true}
      (let [user (int/lookup-user system "joe@example.com")
            handler (-> system
                        (int/component :api/handler#auth)
                        (ihttp/with-serde system :serdes/edn))]
        (stubs/set-stub! (int/component system :services/oauth)
                         :profile
                         (fn [opts]
                           (if (= "secret-pin-12345" (:code opts))
                             {:email (:user/email user)}
                             (throw (ex-info "fail" {})))))
        (testing "when the auth provider interactions succeed"
          (let [response (-> {}
                             (ihttp/get system :auth/callback {:query-params {:code "secret-pin-12345"}})
                             handler)
                base-url (int/component system :env/base-url#ui)
                jwt-serde (int/component system :serdes/jwt)
                cookies (tu/decode-cookies response)]
            (testing "redirects with token cookie"
              (is (http/redirect? response))
              (is (= (str base-url "/")
                     (get-in response [:headers "Location"])))
              (is (= user
                     (-> jwt-serde
                         (serdes/deserialize (get-in cookies ["auth-token" :value]))
                         (dissoc :user/created-at)))))))

        (testing "when the auth provider interactions fail"
          (-> system
              (int/component :services/oauth)
              (stubs/set-stub! :-token nil))
          (testing "redirects with token cookie"
            (let [response (-> {}
                               (ihttp/get system :auth/callback {:query-params {:code "bad-pin"}})
                               handler)
                  base-url (int/component system :env/base-url#ui)
                  cookies (tu/decode-cookies response)
                  location (get-in response [:headers "Location"])]
              (is (http/redirect? response))
              (is (string/starts-with? location base-url))
              (is (= "login-failed" (get-in (uri/parse location) [:query :error-msg])))
              (is (= "" (get-in cookies ["auth-token" :value]))))))))))
