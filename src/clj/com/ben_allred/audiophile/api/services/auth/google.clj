(ns com.ben-allred.audiophile.api.services.auth.google
  (:require
    [clojure.string :as string]
    [com.ben-allred.audiophile.api.services.auth.protocols :as pauth]
    [com.ben-allred.audiophile.common.utils.http :as http]
    [com.ben-allred.audiophile.common.utils.logger :as log]
    [com.ben-allred.audiophile.common.utils.uri :as uri]
    [com.ben-allred.vow.core :as v]
    [integrant.core :as ig]))

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

(defn ^:private token* [provider http-client {:keys [token-uri] :as cfg} opts]
  (-> opts
      (->> (token-request cfg)
           (http/post http-client token-uri))
      (v/then-> (->> (log/spy :info (str provider " token response"))))))

(defn ^:private profile-request [tokens]
  {:query-params (select-keys tokens #{:access_token})
   :headers      {:content-type "application/json"
                  :accept       "application/json"}})

(defn ^:private profile* [provider http-client {:keys [profile-uri] :as cfg} opts]
  (-> (token* provider http-client cfg opts)
      (v/then-> (->> profile-request (http/get http-client profile-uri))
                (->> (log/spy :info (str provider " profile response"))))
      v/deref!))

(deftype GoogleOAuthProvider [http-client cfg]
  pauth/IOAuthProvider
  (-redirect-uri [_ opts]
    (redirect-uri* cfg opts))
  (-profile [this opts]
    (profile* (.getSimpleName (class this)) http-client cfg opts)))

(defmethod ig/init-key ::oauth-provider [_ {:keys [cfg http-client]}]
  (->GoogleOAuthProvider http-client cfg))
