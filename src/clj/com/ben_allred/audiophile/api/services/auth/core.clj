(ns com.ben-allred.audiophile.api.services.auth.core
  (:require
    [com.ben-allred.audiophile.api.services.auth.protocols :as pauth]
    [com.ben-allred.audiophile.api.services.interactors.core :as int]
    [com.ben-allred.audiophile.api.utils.ring :as ring]
    [com.ben-allred.audiophile.common.services.navigation.core :as nav]
    [com.ben-allred.audiophile.common.utils.logger :as log]
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

(defn ^:private fetch-user [email interactor]
  (first (safely! "querying the user from the database"
                  (when email
                    (int/query-one interactor {:user/email email})))))

(defn ^:private request->params [request]
  (or (get-in request [:nav/route :query-params])
      {}))

(defn ^:private params->user [interactor oauth params]
  (some-> params
          (fetch-profile oauth)
          :email
          (fetch-user interactor)))

(defn ^:private home-path [nav error-msg]
  (nav/path-for nav
                :ui/home
                (when error-msg
                  {:query-params {:error-msg error-msg}})))

(defn with-token
  "Adds a cookie to a response"
  ([response]
   (with-token response nil))
  ([response value]
   (let [[value cookie] (if value
                          [value]
                          ["" {:max-age 0}])]
     (assoc response :cookies (ring/->cookie "auth-token" value cookie)))))

(defn ->redirect-url
  "Generates redirect url"
  ([nav base-url]
   (->redirect-url nav base-url nil))
  ([nav base-url error-msg]
   (str base-url (home-path nav error-msg))))

(deftype AuthInteractor [nav oauth interactor]
  pauth/IOAuthProvider
  (-redirect-uri [_ request]
    (some->> request
             request->params
             (redirect-uri* nav oauth)))
  (-profile [_ request]
    (some->> request
             request->params
             (params->user interactor oauth))))

(defmethod ig/init-key ::auth-provider [_ {:keys [nav oauth interactor]}]
  (->AuthInteractor nav oauth interactor))

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
