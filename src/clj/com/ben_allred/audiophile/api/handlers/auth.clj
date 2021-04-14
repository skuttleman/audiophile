(ns com.ben-allred.audiophile.api.handlers.auth
  (:require
    [integrant.core :as ig]))

(defn token->cookie [resp value cookie]
  (->> value
       (assoc {:path "/" :http-only true} :value)
       (merge cookie)
       (assoc-in resp [:cookies "auth-token"])))

(defmethod ig/init-key ::login [_ _]
  (constantly
    [:http.status/internal-server-error
     {:errors [{:message "not implemented"}]}]))

(defmethod ig/init-key ::logout [_ _]
  (constantly
    [:http.status/internal-server-error
     {:errors [{:message "not implemented"}]}]))

(defmethod ig/init-key ::callback [_ _]
  (constantly
    [:http.status/internal-server-error
     {:errors [{:message "not implemented"}]}]))

(defmethod ig/init-key ::details [_ _]
  (fn [request]
    (if-let [user (get-in request [:auth/user :data :user])]
      [:http.status/ok
       {:data user}]
      [:http.status/no-content
       nil
       nil
       (token->cookie nil "" {:max-age 0})])))
