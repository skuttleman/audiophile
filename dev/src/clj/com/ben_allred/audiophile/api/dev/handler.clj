(ns com.ben-allred.audiophile.api.dev.handler
  (:require
    [clojure.string :as string]
    [com.ben-allred.audiophile.api.templates.core :as templates]
    [com.ben-allred.audiophile.common.views.core :as views]
    [integrant.core :as ig]
    [ring.middleware.resource :as res]))

(defn ^:private index [handler]
  (fn [request]
    (if (= "/" (:uri request))
      {:status  200
       :headers {"Content-Type" "text/html"}
       :body    (templates/html [views/app {}])}
      (handler request))))

(defn ^:private resources [handler]
  (fn [request]
    (or (some-> request
                (res/resource-request "public")
                (assoc-in [:headers "X-Foo"] "bar")
                (assoc-in [:headers "Content-Type"]
                          (cond
                            (string/ends-with? (:uri request) ".js") "application/javascript"
                            (string/ends-with? (:uri request) ".css") "text/css"
                            :else "text/plain")))
        (handler request))))

(defmethod ig/init-key ::app [_ {:keys [app]}]
  (-> app
      index
      resources))
