(ns com.ben-allred.audiophile.backend.infrastructure.http.resources
  (:require
    [clojure.string :as string]
    [com.ben-allred.audiophile.backend.infrastructure.http.ring :as ring]
    [com.ben-allred.audiophile.backend.infrastructure.templates.html :as html]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.common.infrastructure.http.core :as http]))

(defn assets
  "Ring handler that returns public/static assets."
  [_]
  (fn [{:keys [uri] :as request}]
    (some-> request
            (ring/resource-request "public")
            (assoc-in [:headers :content-type]
                      (cond
                        (string/ends-with? uri ".js") "application/javascript"
                        (string/ends-with? uri ".css") "text/css"
                        :else "text/plain")))))

(defn health
  "Ring handler for communicating the health of the system."
  [_]
  (constantly [::http/ok {:a :ok}]))

(defn ui
  "Ring handler for dynamically generating html for authorized user."
  [{:keys [api-base auth-base template]}]
  (fn [{:auth/keys [user]}]
    [::http/ok
     (html/render template
                  {:auth/user user
                   :api-base  api-base
                   :auth-base auth-base})
     {:content-type "text/html"}]))
