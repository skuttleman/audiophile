(ns com.ben-allred.audiophile.api.handlers.core
  (:require
    [clojure.string :as string]
    [com.ben-allred.audiophile.api.utils.ring :as ring]
    [com.ben-allred.audiophile.common.utils.http :as http]
    [integrant.core :as ig]))

(defmethod ig/init-key ::router [_ route-table]
  "HTTP router for API server"
  (fn [{:keys [request-method uri] :as request}]
    (let [route (get-in request [:nav/route :handler])
          handler (or (get route-table [(:request-method request) route])
                      (get route-table [:any route]))
          ui (get route-table [:get :ui/home])]
      (cond
        handler
        (handler request)

        (and (= :get request-method)
             (not (string/starts-with? uri "/api")))
        (ui request)

        :else
        [::http/not-found]))))

(defmethod ig/init-key ::app [_ {:keys [middleware router]}]
  "ring handler to be run as webserver"
  (reduce (fn [handler mw]
            (mw handler))
          router
          (concat middleware [ring/wrap-cookies])))
