(ns com.ben-allred.audiophile.api.handlers.auth
  (:require
    [com.ben-allred.audiophile.api.services.auth.core :as auth]
    [com.ben-allred.audiophile.api.utils.ring :as ring]
    [com.ben-allred.audiophile.common.services.serdes.core :as serdes]
    [com.ben-allred.audiophile.common.utils.http :as http]
    [com.ben-allred.audiophile.common.utils.logger :as log]
    [integrant.core :as ig]))

(def ^:private ^:const expire-token-response
  (auth/add-auth-token {:status ::http/no-content}))

(defmethod ig/init-key ::login [_ {:keys [oauth]}]
  (fn [request]
    (ring/redirect (auth/redirect-uri oauth request))))

(defmethod ig/init-key ::logout [_ {:keys [base-url nav]}]
  (fn [_]
    (auth/logout nav base-url)))

(defmethod ig/init-key ::callback [_ {:keys [base-url nav oauth serde]}]
  (fn [request]
    (if-let [token (some->> (auth/profile oauth request)
                            (serdes/serialize serde))]
      (auth/login nav base-url token)
      (auth/logout nav base-url :login-failed))))

(defmethod ig/init-key ::details [_ _]
  (fn [request]
    (if-let [user (:auth/user request)]
      [::http/ok {:data user}]
      expire-token-response)))
