(ns com.ben-allred.audiophile.backend.infrastructure.auth.core
  (:require
    [com.ben-allred.audiophile.backend.api.protocols :as papp]
    [com.ben-allred.audiophile.backend.core.serdes.jwt :as jwt]
    [com.ben-allred.audiophile.backend.domain.interactors.core :as int]
    [com.ben-allred.audiophile.backend.domain.interactors.protocols :as pint]
    [com.ben-allred.audiophile.backend.infrastructure.http.ring :as ring]
    [com.ben-allred.audiophile.common.core.serdes.core :as serdes]
    [com.ben-allred.audiophile.common.core.serdes.impl :as serde]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.common.core.utils.maps :as maps]
    [com.ben-allred.audiophile.common.core.utils.uuids :as uuids]
    [com.ben-allred.audiophile.common.infrastructure.navigation.core :as nav]))

(defmacro ^:private safely! [ctx & body]
  `(log/with-ctx :OAUTH
     (try ~@body
          (catch Throwable ex#
            (log/error ex# "an error occurred:" ~ctx)
            nil))))

(defn ^:private with-token
  "Adds a cookie to a response"
  ([response]
   (with-token response nil))
  ([response value]
   (let [[value cookie] (if value
                          [value]
                          ["" {:max-age 0}])]
     (assoc response :cookies (ring/->cookie "auth-token" value cookie)))))

(defn ^:private home-path [nav params]
  (nav/path-for nav
                :ui/home
                (when params
                  {:params params})))

(defn ^:private ->redirect-url
  "Generates redirect url"
  ([nav base-url]
   (->redirect-url nav base-url nil))
  ([nav base-url params]
   (str base-url (home-path nav params))))

(defn ^:private redirect* [nav oauth base-url jwt-serde base64-serde params]
  (let [claims (some->> params :login-token (serdes/deserialize jwt-serde))
        token (when claims
                (jwt/auth-token jwt-serde (select-keys claims #{:user/id :user/email})))]
    (-> (or (when token (->redirect-url nav base-url))
            (safely! "generating a redirect url to the auth provider"
              (papp/redirect-uri oauth
                                 (maps/update-maybe params
                                                    :state
                                                    (partial serdes/serialize
                                                             base64-serde))))
            (home-path nav {:error-msg :login-failed}))
        ring/redirect
        (cond-> token (with-token token)))))

(defn ^:private fetch-profile [params oauth]
  (safely! "fetching user profile from the OAuth provider"
    (papp/profile oauth params)))

(defn ^:private fetch-user [email interactor]
  (safely! "querying the user from the database"
    (int/query-one interactor {:user/email email})))

(defn ^:private params->token [interactor oauth jwt-serde params]
  (when-let [email (-> params
                       (fetch-profile oauth)
                       :email)]
    (or (some->> (fetch-user email interactor) (jwt/auth-token jwt-serde))
        ;; disable signup flow
        #_(jwt/signup-token jwt-serde {:user/id    (uuids/random)
                                       :user/email email}))))

(deftype AuthInteractor [interactor oauth nav base-url jwt-serde base64-serde]
  pint/IAuthInteractor
  (login [_ params]
    (redirect* nav oauth base-url jwt-serde base64-serde params))
  (logout [_ params]
    (-> nav
        (->redirect-url base-url params)
        ring/redirect
        with-token))
  (callback [_ params]
    (let [token (params->token interactor oauth jwt-serde params)
          url (when token
                (some->> params
                         :state
                         (serdes/deserialize base64-serde)
                         :redirect-uri
                         (str base-url)))]
      (-> (or url
              (->redirect-url nav base-url (when-not token
                                             {:error-msg :login-failed})))
          ring/redirect
          (with-token token)))))

(defn interactor
  "Constructor for [[AuthInteractor]] used to provide authentication interaction flows."
  [{:keys [base-url interactor jwt-serde nav oauth]}]
  (->AuthInteractor interactor oauth nav base-url jwt-serde serde/base64))
