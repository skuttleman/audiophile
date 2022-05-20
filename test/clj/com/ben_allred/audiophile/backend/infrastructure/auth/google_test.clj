(ns ^:unit com.ben-allred.audiophile.backend.infrastructure.auth.google-test
  (:require
    [clojure.string :as string]
    [clojure.test :refer [are deftest is testing]]
    [com.ben-allred.audiophile.backend.api.protocols :as papp]
    [com.ben-allred.audiophile.backend.infrastructure.auth.google :as goog]
    [com.ben-allred.audiophile.common.core.utils.uri :as uri]
    [com.ben-allred.audiophile.common.infrastructure.resources.protocols :as pres]
    [com.ben-allred.audiophile.test.utils.stubs :as stubs]
    [com.ben-allred.vow.core :as v]))

(deftest auth-provider-test
  (testing "GoogleOAuthProvider"
    (let [cfg {:client-id     (str (gensym "client-id"))
               :client-secret (str (gensym "client-secret"))
               :scopes        [(str (gensym "scope")) (str (gensym "scope"))]
               :auth-uri      (str (gensym "auth-uri"))
               :redirect-uri  (str (gensym "redirect-uri"))
               :token-uri     (str (gensym "token-uri"))
               :profile-uri   (str (gensym "profile-uri"))}
          token (str (gensym "token"))]
      (testing "#redirect-uri"
        (let [provider (goog/->GoogleOAuthProvider nil cfg)
              [url query-str] (string/split (papp/redirect-uri provider nil) #"\?")
              query (uri/split-query query-str)]
          (testing "generates the url as expected"
            (is (= (:auth-uri cfg) url))
            (is (= {:client_id       (:client-id cfg)
                    :redirect_uri    (:redirect-uri cfg)
                    :response_type   "code"
                    :access_type     "offline"
                    :approval_prompt "force"
                    :scope           (string/join " " (:scopes cfg))}
                   query)))))

      (testing "#profile"
        (let [http-client (stubs/create (reify pres/IResource
                                          (request! [_ opts]
                                            (if (= :get (:method opts))
                                              (v/resolve {:profile :info})
                                              (v/resolve {:access_token token})))))
              provider (goog/->GoogleOAuthProvider http-client cfg)
              result (papp/profile provider nil)
              [[token-req] [profile-req] :as calls] (stubs/calls http-client :request!)]
          (is (= 2 (count calls)))

          (testing "responds with the user profile"
            (is (= {:profile :info} result)))

          (testing "sends a properly formatted token request"
            (is (= "application/x-www-form-urlencoded" (get-in token-req [:headers :content-type])))
            (is (= "application/json" (get-in token-req [:headers :accept])))
            (is (= :post (:method token-req)))
            (is (= (:token-uri cfg) (:url token-req)))
            (is (= {:grant_type    "authorization_code"
                    :client_id     (:client-id cfg)
                    :client_secret (:client-secret cfg)
                    :redirect_uri  (:redirect-uri cfg)}
                   (:body token-req))))

          (testing "sends a properly formatted profile request"
            (is (= token (get-in profile-req [:params :access_token])))
            (is (= :get (:method profile-req)))
            (is (= (:profile-uri cfg) (:url profile-req))))

          (testing "when generating the token fails"
            (stubs/set-stub! http-client :request! (v/reject {:an :error}))
            (testing "throws an exception"
              (let [ex (is (thrown? Throwable (papp/profile provider nil)))]
                (is (= {:an :error} (:error (ex-data ex)))))))

          (testing "when fetching the profile fails"
            (stubs/set-stub! http-client :request! (fn [opts]
                                                     (if (= :get (:method opts))
                                                       (v/reject {:an :error})
                                                       (v/resolve {:access_token token}))))
            (testing "throws an exception"
              (let [ex (is (thrown? Throwable (papp/profile provider nil)))]
                (is (= {:an :error} (:error (ex-data ex))))))))))))
