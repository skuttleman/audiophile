(ns com.ben-allred.audiophile.api.handlers.auth
  (:require
    [com.ben-allred.audiophile.api.services.auth.core :as auth]
    [com.ben-allred.audiophile.api.services.repositories.users :as users]
    [com.ben-allred.audiophile.api.utils.ring :as ring]
    [com.ben-allred.audiophile.common.services.navigation.core :as nav]
    [com.ben-allred.audiophile.common.services.serdes.core :as serdes]
    [com.ben-allred.audiophile.common.utils.logger :as log]
    [integrant.core :as ig])
  (:import
    (java.net URI)))

(defn ^:private token->cookie [resp value cookie]
  (assoc resp :cookies (ring/->cookie "auth-token" value cookie)))

(defn ^:private redirect
  ([nav route base-url value]
   (redirect nav route base-url value nil))
  ([nav route base-url value cookie]
   (let [path (cond
                (string? route) route
                (or (nil? route) (.isAbsolute (URI. route))) (nav/path-for nav :ui/home)
                :else route)]
     (-> base-url
         (str path)
         ring/redirect
         (token->cookie value cookie)))))

(defn logout!
  "generate a redirect response that removes the auth-token cookie"
  ([nav base-url]
   (logout! nav base-url nil))
  ([nav base-url error-msg]
   (redirect nav (nav/path-for nav :ui/home (when error-msg {:query-params {:error-msg error-msg}})) base-url "" {:max-age 0})))

(defn login!
  "when user is not nil, generate a redirect response with an auth-token cookie
   when user is nil, logout"
  [nav user jwt-serde base-url redirect-uri]
  (cond
    (seq user)
    (redirect nav redirect-uri base-url (serdes/serialize jwt-serde {:user user}))

    :else
    (logout! nav base-url :user-not-found)))

(defmethod ig/init-key ::login [_ {:keys [oauth]}]
  "GET /auth/login - redirect to auth provider"
  (fn [_]
    (ring/redirect (auth/redirect-uri oauth))))

(defmethod ig/init-key ::logout [_ {:keys [base-url nav]}]
  "GET /auth/logout - remove auth-token cookie"
  (fn [_]
    (logout! nav base-url)))

(defmethod ig/init-key ::callback [_ {:keys [base-url jwt-serde nav oauth tx]}]
  "GET /auth/callback - handle redirect from auth provider"
  (fn [request]
    (let [params (get-in request [:nav/route :query-params])
          profile (auth/profile oauth params)]
      (login! nav
              (users/query-by-email tx (:email profile))
              jwt-serde
              base-url
              "/"))))

(defmethod ig/init-key ::details [_ _]
  "GET /auth/details - return current logged on user's details"
  (fn [request]
    (if-let [user (get-in request [:auth/user :data :user])]
      [:http.status/ok
       {:data user}]
      (token->cookie {:status :http.status/no-content} "" {:max-age 0}))))
