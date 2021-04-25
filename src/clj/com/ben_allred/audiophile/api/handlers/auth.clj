(ns com.ben-allred.audiophile.api.handlers.auth
  (:require
    [com.ben-allred.audiophile.api.services.auth.core :as auth]
    [com.ben-allred.audiophile.api.services.repositories.users :as users]
    [com.ben-allred.audiophile.api.utils.ring :as ring]
    [com.ben-allred.audiophile.common.services.navigation.core :as nav]
    [com.ben-allred.audiophile.common.services.serdes.core :as serdes]
    [com.ben-allred.audiophile.common.utils.http :as http]
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
   (redirect nav
             (nav/path-for nav :ui/home (when error-msg
                                          {:query-params {:error-msg error-msg}}))
             base-url
             ""
             {:max-age 0})))

(defmacro unsafe! [ctx & body]
  `(try [~@body]
        (catch Throwable ex#
          (log/error ex# "an error occurred" ~ctx)
          [nil ex#])))

(defn login! [nav jwt-serde base-url user-repo email]
  (if-let [user (first (unsafe! "querying the user from the database"
                         (when email
                           (users/query-by-email user-repo email))))]
    (redirect nav
              (nav/path-for nav :ui/home)
              base-url
              (serdes/serialize jwt-serde {:user user}))
    (logout! nav base-url :login-failed)))

(defmethod ig/init-key ::login [_ {:keys [nav oauth]}]
  "GET /auth/login - redirect to auth provider"
  (fn [_]
    (ring/redirect (or (first (unsafe! "generating a redirect url to the auth provider"
                                (auth/redirect-uri oauth)))
                       (nav/path-for nav
                                     :ui/home
                                     {:query-params {:error-msg :login-failed}})))))

(defmethod ig/init-key ::logout [_ {:keys [base-url nav]}]
  "GET /auth/logout - remove auth-token cookie"
  (fn [_]
    (logout! nav base-url)))

(defmethod ig/init-key ::callback [_ {:keys [base-url jwt-serde nav oauth user-repo]}]
  "GET /auth/callback - handle redirect from auth provider"
  (fn [request]
    (let [params (get-in request [:nav/route :query-params])
          profile (first (unsafe! "fetching user profile from the OAuth provider"
                           (auth/profile oauth params)))]
      (login! nav jwt-serde base-url user-repo (:email profile)))))

(defmethod ig/init-key ::details [_ _]
  "GET /auth/details - return current logged on user's details"
  (fn [request]
    (if-let [user (get-in request [:auth/user :data :user])]
      [::http/ok {:data user}]
      (token->cookie {:status ::http/no-content} "" {:max-age 0}))))
