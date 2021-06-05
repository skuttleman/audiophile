(ns com.ben-allred.audiophile.api.infrastructure.http.resources
  (:require
    [clojure.string :as string]
    [com.ben-allred.audiophile.api.infrastructure.http.ring :as ring]
    [com.ben-allred.audiophile.api.infrastructure.templates.html :as html]
    [com.ben-allred.audiophile.common.core.resources.http :as http]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]))

(defn assets [_]
  (fn [{:keys [uri] :as request}]
    (some-> request
            (ring/resource-request "public")
            (assoc-in [:headers :content-type]
                      (cond
                        (string/ends-with? uri ".js") "application/javascript"
                        (string/ends-with? uri ".css") "text/css"
                        :else "text/plain")))))

(defn health [_]
  (constantly [::http/ok {:a :ok}]))

(defn ui [{:keys [api-base auth-base template]}]
  (fn [{:auth/keys [user]}]
    [::http/ok
     (html/render template
                  {:auth/user user
                   :api-base  api-base
                   :auth-base auth-base})
     {:content-type "text/html"}]))
