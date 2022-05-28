(ns audiophile.backend.infrastructure.auth.core
  (:require
    [audiophile.backend.api.protocols :as papp]
    [audiophile.backend.core.serdes.jwt :as jwt]
    [audiophile.backend.domain.interactors.core :as int]
    [audiophile.backend.domain.interactors.protocols :as pint]
    [audiophile.backend.infrastructure.http.ring :as ring]
    [audiophile.common.core.serdes.core :as serdes]
    [audiophile.common.core.serdes.impl :as serde]
    [audiophile.common.core.utils.logger :as log]
    [audiophile.common.core.utils.maps :as maps]
    [audiophile.common.core.utils.uuids :as uuids]
    [audiophile.common.infrastructure.navigation.core :as nav]))

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
                :routes.ui/home
                (when params
                  {:params params})))

(defn ^:private ->redirect-url
  "Generates redirect url"
  ([nav base-url]
   (->redirect-url nav base-url nil))
  ([nav base-url params]
   (str base-url (home-path nav params))))

(defn ^:private redirect* [nav oauth base-url jwt-serde base64-serde {:keys [state] :as params}]
  (let [token (when-let [claims (some->> params
                                         :login-token
                                         (serdes/deserialize jwt-serde))]
                (jwt/auth-token jwt-serde (select-keys claims #{:user/id :user/email})))
        redirect-uri (:redirect-uri state)
        base-url (if redirect-uri
                   (str base-url redirect-uri)
                   (->redirect-url nav base-url))]
    (-> (or (when token base-url)
            (safely! "generating a redirect url to the auth provider"
              (papp/redirect-uri oauth
                                 (maps/update-maybe params
                                                    :state
                                                    (partial serdes/serialize
                                                             base64-serde))))
            (home-path nav (maps/assoc-maybe {:error-msg :login-failed}
                                             :redirect-uri redirect-uri)))
        ring/redirect
        (cond-> token (with-token token)))))

(defn ^:private fetch-profile [params oauth]
  (safely! "fetching user profile from the OAuth provider"
    (papp/profile oauth params)))

(defn ^:private fetch-user [email interactor]
  (safely! "querying the user from the database"
    (int/query-one interactor {:user/email email})))

(defn ^:private params->token [interactor oauth jwt-serde params signup?]
  (when-let [email (-> params
                       (fetch-profile oauth)
                       :email)]
    (or (some->> (fetch-user email interactor) (jwt/auth-token jwt-serde))
        (when signup?
          (jwt/signup-token jwt-serde {:user/id    (uuids/random)
                                       :user/email email})))))

(defn ^:private auth-interactor#logout [nav base-url params]
  (-> nav
      (->redirect-url base-url params)
      ring/redirect
      with-token))

(defn ^:private auth-interactor#callback
  [interactor oauth nav base-url jwt-serde base64-serde signup? params]
  (let [token (params->token interactor oauth jwt-serde params signup?)
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
        (with-token token))))

(deftype AuthInteractor [interactor oauth nav base-url jwt-serde base64-serde signup?]
  pint/IAuthInteractor
  (login [_ params]
    (redirect* nav oauth base-url jwt-serde base64-serde params))
  (logout [_ params]
    (auth-interactor#logout nav base-url params))
  (callback [_ params]
    (auth-interactor#callback interactor oauth nav base-url jwt-serde base64-serde signup? params)))

(defn interactor
  "Constructor for [[AuthInteractor]] used to provide authentication interaction flows."
  [{:keys [base-url interactor jwt-serde nav oauth signup?]}]
  (->AuthInteractor interactor oauth nav base-url jwt-serde serde/base64 signup?))
