(ns com.ben-allred.audiophile.api.handlers.core
  (:require
    [integrant.core :as ig]
    [ring.middleware.cookies :refer [wrap-cookies]]
    [clojure.string :as string]))

(defmethod ig/init-key ::router [_ route-table]
  (fn [{:keys [request-method uri] :as request}]
    (let [route (get-in request [:nav/route :handler])
          handler (get route-table [(:request-method request) route])
          ui (get route-table [:get :ui/home])]
      (cond
        handler
        (handler request)

        (and (= :get request-method)
             (not (string/starts-with? uri "/api")))
        (ui request)

        :else
        [:http.status/not-found]))))

(defmethod ig/init-key ::app [_ {:keys [middleware router]}]
  (reduce (fn [handler mw]
            (mw handler))
          router
          (concat middleware [wrap-cookies])))
