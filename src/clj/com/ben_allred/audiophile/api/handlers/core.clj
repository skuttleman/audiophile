(ns com.ben-allred.audiophile.api.handlers.core
  (:require
    [integrant.core :as ig]))

(defmethod ig/init-key ::router [_ route-table]
  (fn [request]
    (let [page (:nav/route request)]
      (if-let [handler (get route-table [(:request-method request) (:handler page)])]
        (handler request)
        [:http.status/not-found]))))

(defmethod ig/init-key ::app [_ {:keys [middleware router]}]
  (reduce (fn [handler mw]
            (mw handler))
          router
          middleware))
