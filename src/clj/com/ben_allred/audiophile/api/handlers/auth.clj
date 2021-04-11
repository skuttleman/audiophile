(ns com.ben-allred.audiophile.api.handlers.auth
  (:require
    [integrant.core :as ig]))

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
