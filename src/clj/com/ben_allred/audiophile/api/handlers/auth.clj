(ns com.ben-allred.audiophile.api.handlers.auth
  (:require
    [com.ben-allred.audiophile.api.services.auth.core :as auth]
    [com.ben-allred.audiophile.api.services.repositories.users :as users]
    [com.ben-allred.audiophile.common.services.navigation.core :as nav]
    [com.ben-allred.audiophile.common.services.serdes.core :as serdes]
    [com.ben-allred.audiophile.common.utils.logger :as log]
    [integrant.core :as ig]
    [ring.util.response :as resp])
  (:import
    (java.net URI)))

(defn token->cookie [resp value cookie]
  (->> value
       (assoc {:path "/" :http-only true} :value)
       (merge cookie)
       (assoc-in resp [:cookies "auth-token"])))

(defn redirect
  ([nav route base-url value]
   (redirect nav route base-url value nil))
  ([nav route base-url value cookie]
   (let [path (cond
                (string? route) route
                (or (nil? route) (.isAbsolute (URI. route))) (nav/path-for nav :ui/home)
                :else route)]
     (-> base-url
         (str path)
         resp/redirect
         (token->cookie value cookie)
         log/spy))))

(defn logout!
  ([nav base-url]
   (logout! nav base-url nil))
  ([nav base-url error-msg]
   (redirect nav (nav/path-for nav :ui/home (when error-msg {:query-params {:error-msg error-msg}})) base-url "" {:max-age 0})))

(defn login* [nav user jwt-serde base-url redirect-uri]
  (cond
    (seq user)
    (redirect nav redirect-uri base-url (serdes/serialize jwt-serde {:user user}))

    :else
    (logout! nav base-url :user-not-found)))

(defmethod ig/init-key ::login [_ {:keys [oauth]}]
  (fn [_]
    (resp/redirect (auth/redirect-uri oauth))))

(defmethod ig/init-key ::logout [_ {:keys [base-url nav]}]
  (fn [_]
    (logout! nav base-url)))

(defmethod ig/init-key ::callback [_ {:keys [base-url jwt-serde nav oauth tx]}]
  (fn [request]
    (let [code (get-in request [:nav/route :query-params :code])
          {:keys [access_token]} (auth/token oauth {:code code})
          profile (auth/profile oauth {:token access_token})]
      (login* nav
              (users/query-by-email tx (:email profile))
              jwt-serde
              base-url
              "/"))))

(defmethod ig/init-key ::details [_ _]
  (fn [request]
    (if-let [user (get-in request [:auth/user :data :user])]
      [:http.status/ok
       {:data user}]
      (token->cookie {:status :http.status/no-content} "" {:max-age 0}))))
