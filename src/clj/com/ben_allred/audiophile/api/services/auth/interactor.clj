(ns com.ben-allred.audiophile.api.services.auth.interactor
  (:require
    [com.ben-allred.audiophile.api.services.auth.core :as auth]
    [com.ben-allred.audiophile.api.services.auth.protocols :as pauth]
    [com.ben-allred.audiophile.api.services.repositories.users.model :as users]
    [com.ben-allred.audiophile.api.utils.ring :as ring]
    [com.ben-allred.audiophile.common.services.navigation.core :as nav]
    [com.ben-allred.audiophile.common.utils.logger :as log]
    [integrant.core :as ig]))

(defmacro ^:private safely! [ctx & body]
  `(try [~@body]
        (catch Throwable ex#
          (log/error ex# "an error occurred" ~ctx)
          [nil ex#])))

(defn ^:private redirect-uri [oauth params]
  (first (safely! "generating a redirect url to the auth provider"
                  (auth/redirect-uri oauth params))))

(defn ^:private fetch-profile [params oauth]
  (first (safely! "fetching user profile from the OAuth provider"
                  (auth/profile oauth params))))

(defn ^:private fetch-user [email user-repo]
  (first (safely! "querying the user from the database"
                  (when email
                    (users/query-by-email user-repo email)))))

(defn ^:private request->params [request]
  (get-in request [:nav/route :query-params]))

(defn ^:private params->user [repo oauth params]
  (some-> params
          (fetch-profile oauth)
          :email
          (fetch-user repo)))

(defn ^:private home-path
  ([nav]
   (home-path nav nil))
  ([nav error-msg]
   (nav/path-for nav
                 :ui/home
                 (when error-msg
                   {:query-params {:error-msg error-msg}}))))

(deftype AuthInteractor [oauth repo]
  pauth/IOAuthProvider
  (-redirect-uri [_ request]
    (some->> request
             request->params
             (redirect-uri oauth)))
  (-profile [_ request]
    (some->> request
             request->params
             (params->user repo oauth))))

(defn add-auth-token
  "Adds a cookie to a response"
  ([response]
   (add-auth-token response nil))
  ([response value]
   (let [[value cookie] (if value
                          [value]
                          ["" {:max-age 0}])]
     (assoc response :cookies (ring/->cookie "auth-token" value cookie)))))

(defn login
  "Redirects user to app with an auth token cookie"
  [nav base-url token]
  (-> base-url
      (str (home-path nav))
      ring/redirect
      (add-auth-token token)))

(defn logout
  ([nav base-url]
   (logout nav base-url nil))
  ([nav base-url error-msg]
   (-> base-url
       (str (home-path nav error-msg))
       ring/redirect
       add-auth-token)))

(defmethod ig/init-key ::auth-provider [_ {:keys [oauth repo]}]
  (->AuthInteractor oauth repo))
