(ns com.ben-allred.audiophile.api.handlers.auth
  (:require
    [com.ben-allred.audiophile.api.services.auth.core :as auth]
    [com.ben-allred.audiophile.api.services.auth.interactor :as iauth]
    [com.ben-allred.audiophile.api.utils.ring :as ring]
    [com.ben-allred.audiophile.common.services.navigation.core :as nav]
    [com.ben-allred.audiophile.common.services.serdes.core :as serdes]
    [com.ben-allred.audiophile.common.utils.http :as http]
    [com.ben-allred.audiophile.common.utils.logger :as log]
    [integrant.core :as ig]))

(def ^:private ^:const expire-token-response
  (iauth/add-auth-token {:status ::http/no-content}))

(defmethod ig/init-key ::login [_ {:keys [nav oauth]}]
  (fn [request]
    (-> (auth/redirect-uri oauth request)
        (or (nav/path-for nav
                          :ui/home
                          {:query-params {:error-msg :login-failed}}))
        ring/redirect)))

(defmethod ig/init-key ::logout [_ {:keys [base-url nav]}]
  (fn [_]
    (iauth/logout nav base-url)))

(defmethod ig/init-key ::callback [_ {:keys [base-url nav oauth serde]}]
  (fn [request]
    (if-let [token (some->> (auth/profile oauth request)
                            (serdes/serialize serde))]
      (iauth/login nav base-url token)
      (iauth/logout nav base-url :login-failed))))

(defmethod ig/init-key ::details [_ _]
  (fn [request]
    (if-let [user (:auth/user request)]
      [::http/ok {:data user}]
      expire-token-response)))
