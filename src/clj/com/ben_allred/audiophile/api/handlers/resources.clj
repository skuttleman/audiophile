(ns com.ben-allred.audiophile.api.handlers.resources
  (:require
    [clojure.string :as string]
    [com.ben-allred.audiophile.api.templates.core :as templates]
    [com.ben-allred.audiophile.common.views.core :as views]
    [integrant.core :as ig]
    [ring.middleware.resource :as res]))

(defmethod ig/init-key ::assets [_ _]
  (fn [{:keys [uri] :as request}]
    (some-> request
            (res/resource-request "public")
            (assoc-in [:headers "Content-Type"]
                      (cond
                        (string/ends-with? uri ".js") "application/javascript"
                        (string/ends-with? uri ".css") "text/css"
                        :else "text/plain")))))

(defmethod ig/init-key ::health [_ _]
  (constantly [:http.status/ok {:a :ok}]))

(defmethod ig/init-key ::ui [_ _]
  (constantly [:http.status/ok
               (templates/html [views/app {}])
               {"Content-Type" "text/html"}]))
