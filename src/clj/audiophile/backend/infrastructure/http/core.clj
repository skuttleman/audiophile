(ns audiophile.backend.infrastructure.http.core
  (:require
    [audiophile.backend.api.validations.selectors :as selectors]
    [audiophile.backend.domain.interactors.core :as int]
    [audiophile.backend.infrastructure.http.ring :as ring]
    [audiophile.common.core.utils.fns :as fns]
    [audiophile.common.core.utils.logger :as log]
    [audiophile.common.core.utils.maps :as maps]
    [audiophile.common.domain.validations.core :as val]
    [audiophile.common.infrastructure.http.core :as http]
    [audiophile.common.infrastructure.http.protocols :as phttp]))

(defmulti ex->response (comp :interactor/reason ex-data))

(defmethod ex->response :default
  [ex]
  (log/with-ctx :SERVER
    (log/error ex "An unknown error occurred"))
  (ring/error ::http/internal-server-error "an unknown error occurred"))

(defmethod ex->response int/NO_ACCESS
  [_]
  (ring/error ::http/bad-request "access denied"))

(defmethod ex->response int/INVALID_INPUT
  [_]
  (ring/error ::http/bad-request "invalid request"))

(defmethod ex->response int/NOT_AUTHENTICATED
  [_]
  (ring/error ::http/unauthorized "not authenticated"))

(defmethod ex->response int/INTERNAL_ERROR
  [_]
  (ring/error ::http/internal-server-error "internal server error"))

(defn ^:private route-dispatch [route-table {:keys [request-method] :as request}]
  (let [route (get-in request [:nav/route :handle])]
    (->> [[request-method route]
          [:any route]
          [request-method :routes.ui/home]]
         (filter route-table)
         first)))

(defn ^:private handler->method [route handler]
  (fn [_ request]
    (try (handler (selectors/select route request))
         (catch Throwable ex
           (ex->response ex)))))

(defn router
  "Builds an HTTP router out of a map of handle->handler. Handle should match method+route
   definitions used with [[middleware/with-route]].

   ```clojure
   (let [handler (router {[:get :some/handle] (constantly :get)
                          [:post :some/handle] (constantly :post)
                          [:any :some.other/handle] (constantly :any)})]
     (handler {:nav/route :some.other/handle :request-method :patch}) ;; => :any
   ```"
  [route-table]
  (let [multi (fns/->multi-fn route-dispatch)]
    (doseq [[route handler] (assoc route-table :default (constantly [::http/not-found]))]
      (.addMethod multi route (handler->method route handler)))
    (partial multi route-table)))

(defn app
  "Builds middleware and router into a single function that handles all http requests."
  [{:keys [max-request-size middleware router]}]
  (reduce (fn [handler mw]
            (mw handler))
          router
          (concat middleware [ring/wrap-multipart-params
                              (ring/limit {:max-size max-request-size})
                              ring/wrap-cookies
                              ring/with-logging])))

(defn ^:private data-responder [result]
  (if result
    [::http/ok {:data result}]
    [::http/not-found]))

(defn with-spec
  "Checks input against spec and passes it to the handler and handles exceptions"
  [{:keys [->response handler spec]}]
  (let [->response (or ->response data-responder)]
    (fn [data]
      (try (-> spec
               (val/validate! (maps/dissocp data nil?))
               handler
               ->response)
           (catch Throwable ex
             (let [{:keys [details paths] :as data} (ex-data ex)]
               (log/with-ctx :SERVER
                 (cond
                   (:interactor/reason data)
                   (throw ex)

                   (contains? paths [:user/id])
                   (int/not-authenticated!)

                   details
                   (do (log/warn "Invalid data" spec details)
                       (int/invalid-input!))

                   :else
                   (do (log/error ex "an error occurred")
                       (int/internal-error!))))))))))

(defn ok
  "Wraps result in an http success response"
  [_]
  (fn [[body :as args]]
    (into [(if body
             ::http/ok
             ::http/not-found)]
          args)))

(defn id
  "Expects http response and does not wrap again."
  [_]
  identity)

(defn no-content
  "Ignores result and issues an http no-content response"
  [_]
  (constantly [::http/no-content]))

(defn display-name [component]
  (phttp/display-name component))

(defn healthy? [component]
  (try
    (boolean (phttp/healthy? component))
    (catch Throwable ex
      (log/debug ex "failed to do health check:" (display-name component))
      false)))

(defn details [component]
  (try
    (phttp/details component)
    (catch Throwable ex
      (log/debug ex "failed to obtain health details:" (display-name component))
      nil)))
