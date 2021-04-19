(ns com.ben-allred.audiophile.api.services.auth.google
  (:require
    [clojure.string :as string]
    [com.ben-allred.audiophile.api.services.auth.protocols :as pauth]
    [com.ben-allred.audiophile.common.services.http :as http]
    [com.ben-allred.audiophile.common.utils.uri :as uri]
    [com.ben-allred.vow.core :as v]
    [integrant.core :as ig]))

(defn ^:private redirect-uri* [{:keys [client-id scopes auth-uri redirect-uri]} opts]
  (-> auth-uri
      uri/parse
      (assoc :query (cond-> {:client_id       client-id
                             :redirect_uri    redirect-uri
                             :response_type   "code"
                             :access_type     "offline"
                             :approval_prompt "force"}
                      (:state opts) (assoc :state (:state opts))
                      scopes (assoc :scope (string/join " " scopes))))
      uri/stringify))

(defn ^:private token* [http-client {:keys [client-id client-secret redirect-uri token-uri]} opts]
  (let [request {:headers {:content-type "application/x-www-form-urlencoded"
                           :accept       "application/json"}
                 :body    (uri/join-query (assoc opts
                                                 :grant_type "authorization_code"
                                                 :client_id client-id
                                                 :client_secret client-secret
                                                 :redirect_uri redirect-uri))}]
    (http/post http-client token-uri request)))

(defn ^:private profile* [http-client {:keys [profile-uri] :as cfg} opts]
  (-> (token* http-client cfg opts)
      (v/then (fn [resp]
                (http/get http-client
                          profile-uri
                          {:query-params (select-keys resp #{:access_token})
                           :headers      {:content-type "application/json"
                                          :accept       "application/json"}})))
      v/deref!))

(deftype GoogleOAuthProvider [http-client cfg]
  pauth/IOAuthProvider
  (-redirect-uri [_ opts]
    (redirect-uri* cfg opts))
  (-profile [_ opts]
    (profile* http-client cfg opts)))

(defmethod ig/init-key ::oauth-provider [_ {:keys [cfg http-client]}]
  (->GoogleOAuthProvider http-client cfg))
