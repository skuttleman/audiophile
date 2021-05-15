(ns com.ben-allred.audiophile.api.handlers.middleware
  (:require
    [camel-snake-kebab.core :as csk]
    [clojure.core.match :as match]
    [com.ben-allred.audiophile.common.services.navigation.core :as nav]
    [com.ben-allred.audiophile.common.services.serdes.core :as serdes]
    [com.ben-allred.audiophile.common.utils.http :as http]
    [com.ben-allred.audiophile.common.utils.logger :as log]
    [com.ben-allred.audiophile.common.utils.maps :as maps]
    [integrant.core :as ig])
  (:import
    (java.io File InputStream)
    (org.projectodd.wunderboss.web.async Channel)))

(defn ^:private ->response [response]
  (-> response
      (match/match
        nil {:status (http/status->code ::http/not-found)}
        [status] {:status (http/status->code status)}
        [status body] {:status (http/status->code status) :body body}
        [status body headers] {:status  (http/status->code status)
                               :body    body
                               :headers headers}
        ({:status (_ :guard keyword?)} :as response) (update response :status http/status->code)
        response response)
      (vary-meta merge (meta response))))

(defn ^:private serializable? [resp]
  (and (some? resp)
       (empty? (filter #(instance? % resp)
                       [File InputStream Channel String]))))

(defmethod ig/init-key ::vector-response [_ _]
  (fn [handler]
    (fn [request]
      (-> request handler ->response))))

(defmethod ig/init-key ::serde [_ serdes]
  (fn [handler]
    (fn [request]
      (let [request-serde (serdes/find-serde serdes
                                             (get-in request [:headers :content-type]))
            response (-> request
                         (cond-> request-serde (maps/update-maybe :body (partial serdes/deserialize request-serde)))
                         handler)
            response-serde (serdes/find-serde serdes
                                              (or (get-in request [:headers :accept])
                                                  (get-in response [:headers :content-type])
                                                  "unknown/mime-type")
                                              request-serde)]
        (cond-> response
          (and response-serde (serializable? (:body response)))
          (-> (update :body (partial serdes/serialize response-serde))
              (assoc-in [:headers :content-type] (serdes/mime-type response-serde))))))))

(defmethod ig/init-key ::with-route [_ {:keys [nav]}]
  (fn [handler]
    (fn [{:keys [query-string uri] :as request}]
      (-> request
          (assoc :nav/route (nav/match-route nav
                                             (cond-> uri
                                               query-string (str "?" query-string))))
          handler))))

(defn ^:private log-color [elapsed]
  (cond
    (>= elapsed 1000000000) "\u001B[31m"
    (>= elapsed 100000000) "\u001B[33;1m"
    (>= elapsed 1000000) "\u001b[37;1m"
    (>= elapsed 1000) "\u001b[36m"
    :else "\u001b[32;1m"))

(defn ^:private log-time [elapsed]
  (let [color (log-color elapsed)]
    (cond
      (>= elapsed 1000000000) (str color (long (/ elapsed 1000000000)) "s\u001B[0m")
      (>= elapsed 1000000) (str color (long (/ elapsed 1000000)) "ms\u001B[0m")
      (>= elapsed 1000) (str color (long (/ elapsed 1000)) "μs\u001B[0m")
      :else (str color elapsed "ns\u001B[0m"))))

(defn ^:private log-msg [request response elapsed]
  (let [time (log-time elapsed)]
    (format "%s %s [%s] - %d"
            (csk/->SCREAMING_SNAKE_CASE_STRING (:request-method request))
            (:uri request)
            time
            (:status response))))

(defn ^:private with-logging [handler request]
  (let [start (System/nanoTime)
        response (handler request)
        end (System/nanoTime)
        elapsed (- end start)
        msg (log-msg request response elapsed)]
    (if (::ex (meta response))
      (log/warn msg)
      (log/info msg))
    response))

(defmethod ig/init-key ::with-logging [_ _]
  (fn [handler]
    (fn [request]
      (if (some-> request
                  (get-in [:nav/route :handle])
                  ((some-fn #{:resources/health}
                            (comp #{"auth" "api"} namespace))))
        (with-logging handler request)
        (handler request)))))

(defmethod ig/init-key ::with-auth [_ {:keys [jwt-serde]}]
  (fn [handler]
    (fn [request]
      (let [jwt (get-in request [:cookies "auth-token" :value])
            user (:data (serdes/deserialize jwt-serde jwt))]
        (-> request
            (maps/assoc-maybe :auth/user user)
            handler)))))

(defmethod ig/init-key ::with-headers [_ _]
  (fn [handler]
    (fn [request]
      (-> request
          (maps/update-maybe :headers (partial maps/map-keys csk/->kebab-case-keyword))
          handler
          (maps/update-maybe :headers (partial maps/map-keys name))))))
