(ns com.ben-allred.audiophile.api.infrastructure.http.core
  (:require
    [com.ben-allred.audiophile.api.domain.interactors.core :as int]
    [com.ben-allred.audiophile.api.domain.validations.selectors :as selectors]
    [com.ben-allred.audiophile.api.infrastructure.http.ring :as ring]
    [com.ben-allred.audiophile.common.core.resources.http :as http]
    [com.ben-allred.audiophile.common.core.utils.fns :as fns]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.common.domain.validations.core :as val]))

(defmulti ex->response (comp :interactor/reason ex-data))

(defmethod ex->response :default
  [ex]
  (log/error ex "An unknown error occurred")
  (ring/error ::http/internal-server-error "an unknown error occurred"))

(defmethod ex->response int/INVALID_INPUT
  [_]
  (ring/error ::http/bad-request "invalid request"))

(defmethod ex->response int/NOT_AUTHENTICATED
  [_]
  (ring/error ::http/unauthorized "not authenticated"))

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
    (try (handler (selectors/select route request))
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

(defn ^:private data-responder [result]
  (if result
    [::http/ok {:data result}]
    [::http/not-found]))

(defn with-spec [{:keys [->response handler spec]}]
  (let [->response (or ->response data-responder)]
    (fn [data]
      (try (-> spec
               (val/validate! data)
               handler
               ->response)
           (catch Throwable ex
             (if (contains? (:paths (ex-data ex))
                            [:user/id])
               (int/not-authenticated!)
               (int/invalid-input!)))))))

(defn ok [_]
  (partial into [::http/ok]))

(defn id [_]
  identity)
