(ns com.ben-allred.audiophile.backend.infrastructure.auth.core
  (:require
    [com.ben-allred.audiophile.backend.api.protocols :as papp]
    [com.ben-allred.audiophile.backend.domain.interactors.core :as int]
    [com.ben-allred.audiophile.backend.domain.interactors.protocols :as pint]
    [com.ben-allred.audiophile.backend.infrastructure.http.ring :as ring]
    [com.ben-allred.audiophile.common.api.navigation.core :as nav]
    [com.ben-allred.audiophile.common.core.serdes.core :as serdes]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]))

(defmacro ^:private safely! [ctx & body]
  `(try [~@body]
        (catch Throwable ex#
          (log/error ex# "an error occurred" ~ctx)
          [nil ex#])))

(defn ^:private with-token
  "Adds a cookie to a response"
  ([response]
   (with-token response nil))
  ([response value]
   (let [[value cookie] (if value
                          [value]
                          ["" {:max-age 0}])]
     (assoc response :cookies (ring/->cookie "auth-token" value cookie)))))

(defn ^:private redirect-uri* [nav oauth params]
  (or (first (safely! "generating a redirect url to the auth provider"
                      (papp/redirect-uri oauth params)))
      (nav/path-for nav
                    :ui/home
                    {:query-params {:error-msg :login-failed}})))

(defn ^:private fetch-profile [params oauth]
  (first (safely! "fetching user profile from the OAuth provider"
                  (papp/profile oauth params))))

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

(defn ^:private ->redirect-url
  "Generates redirect url"
  ([nav base-url]
   (->redirect-url nav base-url nil))
  ([nav base-url error-msg]
   (str base-url (home-path nav error-msg))))

(deftype AuthInteractor [interactor oauth nav jwt-serde base-url]
  pint/IAuthInteractor
  (login [_ params]
    (ring/redirect (some->> params
                            request->params
                            (redirect-uri* nav oauth))))
  (logout [_ _]
    (-> nav
        (->redirect-url base-url)
        ring/redirect
        with-token))
  (callback [_ params]
    (let [token (some->> params
                         request->params
                         (params->user interactor oauth)
                         (serdes/serialize jwt-serde))]
      (-> nav
          (->redirect-url base-url (when-not token :login-failed))
          ring/redirect
          (with-token token)))))

(defn interactor
  "Constructor for [[AuthInteractor]] used to provide authentication interaction flows."
  [{:keys [base-url interactor jwt-serde nav oauth]}]
  (->AuthInteractor interactor oauth nav jwt-serde base-url))
