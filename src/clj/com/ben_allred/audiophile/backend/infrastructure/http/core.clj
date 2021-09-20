(ns com.ben-allred.audiophile.backend.infrastructure.http.core
  (:require
    [com.ben-allred.audiophile.backend.api.validations.selectors :as selectors]
    [com.ben-allred.audiophile.backend.domain.interactors.core :as int]
    [com.ben-allred.audiophile.common.infrastructure.http.protocols :as phttp]
    [com.ben-allred.audiophile.backend.infrastructure.http.ring :as ring]
    [com.ben-allred.audiophile.common.core.utils.fns :as fns]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.common.core.utils.maps :as maps]
    [com.ben-allred.audiophile.common.domain.validations.core :as val]
    [com.ben-allred.audiophile.common.infrastructure.http.core :as http]))

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

(defmethod ex->response int/INTERNAL_ERROR
  [_]
  (ring/error ::http/internal-server-error "internal server error"))

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

(defn router
  "Builds an http router out of a map of handle->handler. Handle should match method+route
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
  [{:keys [middleware router]}]
  (reduce (fn [handler mw]
            (mw handler))
          router
          (concat middleware [ring/wrap-multipart-params ring/wrap-cookies])))

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
             (let [{:keys [details paths]} (ex-data ex)]
               (if (contains? paths [:user/id])
                 (int/not-authenticated!)
                 (do (if details
                       (log/warn "Invalid data" spec details)
                       (log/error ex spec))
                     (int/invalid-input!)))))))))

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
    (phttp/healthy? component)
    (catch Exception ex
      (log/debug ex "failed to do health check:" (display-name component))
      false)))

(defn details [component]
  (phttp/details component))
