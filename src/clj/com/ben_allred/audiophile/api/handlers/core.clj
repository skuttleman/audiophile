(ns com.ben-allred.audiophile.api.handlers.core
  (:require
    [com.ben-allred.audiophile.api.handlers.validations.core :as validations]
    [com.ben-allred.audiophile.api.utils.ring :as ring]
    [com.ben-allred.audiophile.common.utils.fns :as fns]
    [com.ben-allred.audiophile.common.utils.http :as http]
    [com.ben-allred.audiophile.common.utils.logger :as log]
    [integrant.core :as ig]))

(defn ^:private pre-req-dispatch [pre-req _ _] pre-req)

(defmulti ^:private pre-req! pre-req-dispatch)

(defmethod pre-req! :default
  [_ _ _])

(defmethod pre-req! :auth/user
  [_ _ request]
  (when (nil? (:auth/user request))
    (ring/abort! "you must be logged in" ::http/unauthorized)))

(defn ^:private route-dispatch [route-table request]
  (let [route (get-in request [:nav/route :handler])]
    (->> [[(:request-method request) route]
          [:any route]
          [(:request-method request) :ui/home]]
         (filter route-table)
         first)))

(defn ^:private handler->method [route handler]
  (fn [_ request]
    (doseq [[k v] (meta route)]
      (pre-req! k v request))
    (handler (validations/validate! route request))))

(defmethod ig/init-key ::router [_ route-table]
  (let [multi (fns/->multi-fn route-dispatch)]
    (doseq [[route handler] (assoc route-table :default (constantly [::http/not-found]))]
      (.addMethod multi route (handler->method route handler)))
    (partial multi route-table)))

(defmethod ig/init-key ::app [_ {:keys [middleware router]}]
  (reduce (fn [handler mw]
            (mw handler))
          router
          (concat middleware [ring/wrap-multipart-params ring/wrap-cookies])))
