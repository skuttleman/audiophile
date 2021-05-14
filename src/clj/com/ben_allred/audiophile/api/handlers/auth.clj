(ns com.ben-allred.audiophile.api.handlers.auth
  (:require
    [com.ben-allred.audiophile.api.services.auth.core :as auth]
    [com.ben-allred.audiophile.api.utils.ring :as ring]
    [com.ben-allred.audiophile.common.services.serdes.core :as serdes]
    [com.ben-allred.audiophile.common.utils.http :as http]
    [com.ben-allred.audiophile.common.utils.logger :as log]
    [integrant.core :as ig]))

(def ^:private ^:const expire-token-response
  (auth/with-token {:status ::http/no-content}))

(defn ^:private redirect!
  ([nav base-url]
   (redirect! nav base-url nil))
  ([nav base-url token]
   (-> nav
       (auth/->redirect-url base-url (when-not token :login-failed))
       ring/redirect
       (auth/with-token token))))

(defmethod ig/init-key ::login [_ {:keys [oauth]}]
  (fn [request]
    (ring/redirect (auth/redirect-uri oauth request))))

(defmethod ig/init-key ::logout [_ {:keys [base-url nav]}]
  (fn [_]
    (redirect! nav base-url)))

(defmethod ig/init-key ::callback [_ {:keys [base-url nav oauth serde]}]
  (fn [request]
    (let [token (some->> (auth/profile oauth request)
                         (serdes/serialize serde))]
      (redirect! nav base-url token))))

(defmethod ig/init-key ::details [_ _]
  (fn [request]
    (if-let [user (:auth/user request)]
      [::http/ok {:data user}]
      expire-token-response)))
