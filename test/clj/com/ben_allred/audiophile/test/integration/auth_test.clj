(ns ^:integration com.ben-allred.audiophile.test.integration.auth-test
  (:require
    [clojure.string :as string]
    [clojure.test :refer [are deftest is testing use-fixtures]]
    [com.ben-allred.audiophile.backend.core.serdes.jwt :as jwt]
    [com.ben-allred.audiophile.common.core.serdes.core :as serdes]
    [com.ben-allred.audiophile.common.core.serdes.impl :as serde]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.common.core.utils.uri :as uri]
    [com.ben-allred.audiophile.common.core.utils.uuids :as uuids]
    [com.ben-allred.audiophile.common.infrastructure.http.core :as http]
    [com.ben-allred.audiophile.test.integration.common :as int]
    [com.ben-allred.audiophile.test.integration.common.http :as ihttp]
    [com.ben-allred.audiophile.test.utils :as tu]
    [com.ben-allred.audiophile.test.utils.assertions :as assert]
    [com.ben-allred.audiophile.test.utils.stubs :as stubs]))

(deftest auth-callback-test
  (testing "GET /auth/callback"
    (int/with-config [system [:api/handler]]
      (let [user (int/lookup-user system "joe@example.com")
            handler (-> system
                        (int/component :api/handler)
                        (ihttp/with-serde serde/transit))]
        (stubs/set-stub! (int/component system :services/oauth)
                         :profile
                         (fn [opts]
                           (case (:code opts)
                             "secret-pin-12345" {:email (:user/email user)}
                             "another-pin" {:email "unknown@user.com"})))
        (testing "when the auth provider interactions succeed"
          (let [response (-> {}
                             (ihttp/get system :auth/callback {:params {:code "secret-pin-12345"}})
                             handler)
                base-url (int/component system :env/base-url#ui)
                jwt-serde (int/component system :serdes/jwt)
                cookies (tu/decode-cookies response)]
            (testing "redirects with an auth token cookie"
              (is (http/redirect? response))
              (is (= (str base-url "/")
                     (get-in response [:headers "Location"])))
              (assert/is? {:user/id (:user/id user)}
                          (-> jwt-serde
                              (serdes/deserialize (get-in cookies ["auth-token" :value])))))))

        (testing "when the auth provider provides an unknown profile"
          (let [response (-> {}
                             (ihttp/get system :auth/callback {:params {:code "another-pin"}})
                             handler)
                base-url (int/component system :env/base-url#ui)
                jwt-serde (int/component system :serdes/jwt)
                cookies (tu/decode-cookies response)]
            (testing "redirects with a signup token cookie"
              (is (http/redirect? response))
              (is (= (str base-url "/")
                     (get-in response [:headers "Location"])))
              (assert/is? {:jwt/aud #{:token/signup}
                           :user/id uuid?
                           :user/email "unknown@user.com"}
                          (serdes/deserialize jwt-serde (get-in cookies ["auth-token" :value]))))))

        (testing "when the auth provider interactions fail"
          (-> system
              (int/component :services/oauth)
              (stubs/set-stub! :-token nil))
          (testing "redirects and removes token cookie"
            (let [response (-> {}
                               (ihttp/get system :auth/callback {:params {:code "bad-pin"}})
                               handler)
                  base-url (int/component system :env/base-url#ui)
                  cookies (tu/decode-cookies response)
                  location (get-in response [:headers "Location"])]
              (is (http/redirect? response))
              (is (string/starts-with? location base-url))
              (is (= "login-failed" (get-in (uri/parse location) [:query :error-msg])))
              (is (= "" (get-in cookies ["auth-token" :value]))))))))))

(deftest auth-login-test
  (testing "GET /auth/login"
    (int/with-config [system [:api/handler]]
      (let [user (int/lookup-user system "joe@example.com")
            handler (-> system
                        (int/component :api/handler)
                        (ihttp/with-serde serde/transit))]
        (testing "when logging in with a login token"
          (let [jwt-serde (int/component system :serdes/jwt)
                login-token (jwt/login-token jwt-serde user)
                response (-> {}
                             (ihttp/get system :auth/login {:params {:login-token login-token}})
                             handler)
                base-url (int/component system :env/base-url#ui)
                cookies (tu/decode-cookies response)]
            (testing "redirects with token cookie"
              (is (http/redirect? response))
              (is (= (str base-url "/")
                     (get-in response [:headers "Location"])))
              (assert/is? {:user/id (:user/id user)
                           :jwt/aud #{:token/auth}}
                          (-> jwt-serde
                              (serdes/deserialize (get-in cookies ["auth-token" :value])))))))))))

(deftest auth-signup-test
  (testing "GET /api/users"
    (int/with-config [system [:api/handler]]
      (let [handler (-> system
                        (int/component :api/handler)
                        (ihttp/with-serde serde/transit))
            signup {:user/id (uuids/random)
                    :user/email "new@user.com"
                    :user/mobile-number "9876543210"
                    :user/first-name "Bluff"
                    :user/last-name  "Tuffington"
                    :user/handle     "thebluffster"}
            jwt-serde (int/component system :serdes/jwt)]
        (testing "when logging in with a login token"
          (let [response (-> signup
                             (dissoc :user/id :user/email)
                             ihttp/body-data
                             (ihttp/login system signup {:jwt/claims {:aud #{:token/signup}}})
                             (ihttp/post system :api/users)
                             (ihttp/as-async system handler))
                token (get-in response [:body :data :login/token])
                claims (serdes/deserialize jwt-serde token)]
            (testing "produces a login token"
              (is (http/success? response))
              (assert/is? {:jwt/aud #{:token/login}
                           :user/id uuid?}
                          claims))))

        (testing "when signing up fails"
          (let [signup {:user/id (uuids/random)
                        :user/email "new@user.com"
                        :user/mobile-number "1234567890"
                        :user/first-name "Bluff"
                        :user/last-name  "Tuffington"
                        :user/handle     "thebluffster"}
                response (-> signup
                             (dissoc :user/id :user/email)
                             ihttp/body-data
                             (ihttp/login system signup {:jwt/claims {:aud #{:token/signup}}})
                             (ihttp/post system :api/users)
                             (ihttp/as-async system handler))]
            (testing "returns an error"
              (is (http/client-error? response))
              (is (= :user/create!
                     (get-in response [:body :data :error/command]))))))))))
