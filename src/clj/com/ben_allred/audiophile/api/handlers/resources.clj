(ns com.ben-allred.audiophile.api.handlers.resources
  (:require
    [clojure.string :as string]
    [com.ben-allred.audiophile.api.templates.core :as templates]
    [com.ben-allred.audiophile.api.utils.ring :as ring]
    [com.ben-allred.audiophile.common.services.ui-store.core :as ui-store]
    [com.ben-allred.audiophile.common.utils.http :as http]
    [integrant.core :as ig]))

(defmethod ig/init-key ::assets [_ _]
  (fn [{:keys [uri] :as request}]
    (some-> request
            (ring/resource-request "public")
            (assoc-in [:headers :content-type]
                      (cond
                        (string/ends-with? uri ".js") "application/javascript"
                        (string/ends-with? uri ".css") "text/css"
                        :else "text/plain")))))

(defmethod ig/init-key ::health [_ _]
  (constantly [::http/ok {:a :ok}]))

(defmethod ig/init-key ::ui [_ {:keys [store app]}]
  (fn [_]
    [::http/ok
     (templates/html [app (ui-store/get-state store)])
     {:content-type "text/html"}]))
