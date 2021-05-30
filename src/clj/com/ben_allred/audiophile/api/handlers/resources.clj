(ns com.ben-allred.audiophile.api.handlers.resources
  (:require
    [clojure.string :as string]
    [com.ben-allred.audiophile.api.templates.core :as templates]
    [com.ben-allred.audiophile.api.utils.ring :as ring]
    [com.ben-allred.audiophile.common.services.ui-store.core :as ui-store]
    [com.ben-allred.audiophile.common.utils.http :as http]
    [com.ben-allred.audiophile.common.utils.logger :as log]
    [com.ben-allred.audiophile.common.utils.maps :as maps]))

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

(defn ui [{:keys [api-base app auth-base store]}]
  (fn [{user :auth/user route :nav/route}]
    [::http/ok
     (templates/html [app (maps/assoc-maybe (ui-store/get-state store)
                                            :auth/user user
                                            :nav/route route)]
                     {:auth/user user
                      :api-base  api-base
                      :auth-base auth-base})
     {:content-type "text/html"}]))
