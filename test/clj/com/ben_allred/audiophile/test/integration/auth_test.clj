(ns ^:integration com.ben-allred.audiophile.test.integration.auth-test
  (:require
    [clojure.string :as string]
    [clojure.test :refer [are deftest is testing use-fixtures]]
    [com.ben-allred.audiophile.common.core.serdes.core :as serdes]
    [com.ben-allred.audiophile.common.infrastructure.http.core :as http]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.common.core.utils.uri :as uri]
    [com.ben-allred.audiophile.test.integration.common :as int]
    [com.ben-allred.audiophile.test.integration.common.http :as ihttp]
    [com.ben-allred.audiophile.test.utils :as tu]
    [com.ben-allred.audiophile.test.utils.assertions :as assert]
    [com.ben-allred.audiophile.test.utils.stubs :as stubs]))

(deftest auth-callback-test
  (testing "GET /auth/callback"
    (int/with-config [system [:api/handler]] {:db/enabled? true}
      (let [user (int/lookup-user system "joe@example.com")
            handler (-> system
                        (int/component :api/handler)
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
              (assert/is? {:user/id (:user/id user)}
                          (-> jwt-serde
                              (serdes/deserialize (get-in cookies ["auth-token" :value])))))))

        (testing "when the auth provider interactions fail"
          (-> system
              (int/component :services/oauth)
              (stubs/set-stub! :-token nil))
          (testing "redirects and removes token cookie"
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