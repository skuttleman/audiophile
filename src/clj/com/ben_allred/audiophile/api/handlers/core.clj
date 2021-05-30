(ns com.ben-allred.audiophile.api.handlers.core
  (:require
    [com.ben-allred.audiophile.api.handlers.validations.core :as validations]
    [com.ben-allred.audiophile.api.services.interactors.core :as int]
    [com.ben-allred.audiophile.api.utils.ring :as ring]
    [com.ben-allred.audiophile.common.utils.fns :as fns]
    [com.ben-allred.audiophile.common.utils.http :as http]
    [com.ben-allred.audiophile.common.utils.logger :as log]))

(defmulti ex->response (comp :interactor/reason ex-data))

(defmethod ex->response :default
  [ex]
  (log/error ex "An unknown error occurred")
  (ring/error ::http/internal-server-error "an unknown error occurred"))

(defmethod ex->response int/INVALID_INPUT
  [_]
  (ring/error ::http/bad-request "invalid request"))

(defmethod ex->response int/NOT_IMPLEMENTED
  [_]
  (ring/error ::http/not-implemented "not implemented"))

(defn ^:private route-dispatch [route-table request]
  (let [route (get-in request [:nav/route :handle])]
    (->> [[(:request-method request) route]
          [:any route]
          [(:request-method request) :ui/home]]
         (filter route-table)
         first)))

(defn ^:private handler->method [route handler]
  (fn [_ request]
    (try (handler (validations/select-input route request))
         (catch Throwable ex
           (ex->response ex)))))

(defn router [route-table]
  (let [multi (fns/->multi-fn route-dispatch)]
    (doseq [[route handler] (assoc route-table :default (constantly [::http/not-found]))]
      (.addMethod multi route (handler->method route handler)))
    (partial multi route-table)))

(defn app [{:keys [middleware router]}]
  (reduce (fn [handler mw]
            (mw handler))
          router
          (concat middleware [ring/wrap-multipart-params ring/wrap-cookies])))
