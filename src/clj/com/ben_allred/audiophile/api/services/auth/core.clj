(ns com.ben-allred.audiophile.api.services.auth.core
  (:require
    [com.ben-allred.audiophile.api.services.auth.protocols :as pauth]
    [com.ben-allred.audiophile.common.utils.logger :as log]
    [com.ben-allred.audiophile.api.services.interactors.users :as users]
    [com.ben-allred.audiophile.common.services.navigation.core :as nav]
    [com.ben-allred.audiophile.api.utils.ring :as ring]
    [integrant.core :as ig]))

(defmacro ^:private safely! [ctx & body]
  `(try [~@body]
        (catch Throwable ex#
          (log/error ex# "an error occurred" ~ctx)
          [nil ex#])))

(defn ^:private redirect-uri* [nav oauth params]
  (or (first (safely! "generating a redirect url to the auth provider"
                      (pauth/-redirect-uri oauth params)))
      (nav/path-for nav
                    :ui/home
                    {:query-params {:error-msg :login-failed}})))

(defn ^:private fetch-profile [params oauth]
  (first (safely! "fetching user profile from the OAuth provider"
                  (pauth/-profile oauth params))))

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
  "Redirects user to UI app with an auth token cookie"
  [nav base-url token]
  (-> base-url
      (str (home-path nav))
      ring/redirect
      (add-auth-token token)))

(defn logout
  "Redirects user to UI app without an auth token cookie"
  ([nav base-url]
   (logout nav base-url nil))
  ([nav base-url error-msg]
   (-> base-url
       (str (home-path nav error-msg))
       ring/redirect
       add-auth-token)))

(deftype AuthInteractor [nav oauth repo]
  pauth/IOAuthProvider
  (-redirect-uri [_ request]
    (some->> request
             request->params
             (redirect-uri* nav oauth)))
  (-profile [_ request]
    (some->> request
             request->params
             (params->user repo oauth))))

(defmethod ig/init-key ::auth-provider [_ {:keys [nav oauth repo]}]
  (->AuthInteractor nav oauth repo))

(defn redirect-uri
  ([provider]
   (redirect-uri provider nil))
  ([provider opts]
   (pauth/-redirect-uri provider opts)))

(defn profile
  ([provider]
   (profile provider nil))
  ([provider opts]
   (pauth/-profile provider opts)))
