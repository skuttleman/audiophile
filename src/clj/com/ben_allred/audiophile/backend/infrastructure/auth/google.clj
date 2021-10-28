(ns com.ben-allred.audiophile.backend.infrastructure.auth.google
  (:require
    [clojure.string :as string]
    [com.ben-allred.audiophile.backend.api.protocols :as papp]
    [com.ben-allred.audiophile.common.infrastructure.http.core :as http]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.common.core.utils.uri :as uri]
    [com.ben-allred.vow.core :as v]))

(defn ^:private redirect-params [{:keys [client-id redirect-uri]}]
  {:client_id       client-id
   :redirect_uri    redirect-uri
   :response_type   "code"
   :access_type     "offline"
   :approval_prompt "force"})

(defn ^:private redirect-uri* [{:keys [scopes auth-uri] :as cfg} opts]
  (-> auth-uri
      uri/parse
      (assoc :query (cond-> (redirect-params cfg)
                      (:state opts) (assoc :state (:state opts))
                      scopes (assoc :scope (string/join " " scopes))))
      uri/stringify))

(defn ^:private token-request [{:keys [client-id client-secret redirect-uri]} opts]
  {:headers {:content-type "application/x-www-form-urlencoded"
             :accept       "application/json"}
   :body    (assoc opts
                   :grant_type "authorization_code"
                   :client_id client-id
                   :client_secret client-secret
                   :redirect_uri redirect-uri)})

(defn ^:private token* [http-client {:keys [token-uri] :as cfg} opts]
  (->> opts
       (token-request cfg)
       (http/post http-client token-uri)))

(defn ^:private profile-request [tokens]
  {:params  (select-keys tokens #{:access_token})
   :headers {:content-type "application/json"
             :accept       "application/json"}})

(defn ^:private profile* [http-client {:keys [profile-uri] :as cfg} opts]
  (let [result (-> http-client
                   (token* cfg opts)
                   (v/then-> profile-request (->> (http/get http-client profile-uri)))
                   v/deref!)]
    (log/info "profile response" result)
    result))

(deftype GoogleOAuthProvider [http-client cfg]
  papp/IOAuthProvider
  (redirect-uri [_ opts]
    (redirect-uri* cfg opts))
  (profile [this opts]
    (log/with-ctx [this :OAUTH]
      (profile* http-client cfg opts))))

(defn provider
  "Constructor for [[GoogleOAuthProvider]] used to provide asynchronous authentication flows."
  [{:keys [cfg http-client]}]
  (->GoogleOAuthProvider http-client cfg))
