(ns audiophile.backend.infrastructure.http.middleware
  (:require
    [camel-snake-kebab.core :as csk]
    [clojure.core.match :as match]
    [clojure.string :as string]
    [audiophile.common.core.serdes.core :as serdes]
    [audiophile.common.core.serdes.impl :as serde]
    [audiophile.common.core.utils.logger :as log]
    [audiophile.common.core.utils.maps :as maps]
    [audiophile.common.infrastructure.http.core :as http]
    [audiophile.common.infrastructure.navigation.core :as nav])
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

(defn vector-response
  "Ring middleware that turns response vectors into a ring-compatible response map."
  [_]
  (fn [handler]
    (fn [request]
      (-> request handler ->response))))

(defn with-serde
  "Ring middleware that serializes/deserializes http body in request/response."
  [_]
  (let [serdes {:edn     serde/edn
                :transit serde/transit}]
    (fn [handler]
      (fn [request]
        (let [request-serde (serdes/find-serde serdes
                                               (get-in request [:headers :content-type]))
              response (-> request
                           (cond-> request-serde (maps/update-maybe :body (partial serdes/deserialize request-serde)))
                           handler)
              response-serde (serdes/find-serde serdes
                                                (or (get-in response [:headers :content-type])
                                                    (get-in request [:headers :accept])
                                                    "unknown/mime-type")
                                                request-serde)]
          (cond-> response
            (serializable? (:body response))
            (-> (update :body (if response-serde
                                (partial serdes/serialize response-serde)
                                str))
                (assoc-in [:headers :content-type] (if response-serde
                                                     (serdes/mime-type response-serde)
                                                     "text/plain")))))))))

(defn with-route
  "Ring middleware that adds route info to the http request map."
  [{:keys [nav]}]
  (fn [handler]
    (fn [{:keys [query-string uri] :as request}]
      (-> request
          (assoc :nav/route (nav/match-route nav
                                             (cond-> uri
                                               query-string (str "?" query-string))))
          handler))))

(defn with-auth
  "Ring middleware to decodes JWT in request headers."
  [{:keys [jwt-serde]}]
  (fn [handler]
    (fn [request]
      (let [jwt (get-in request [:cookies "auth-token" :value])
            user (serdes/deserialize jwt-serde jwt)]
        (-> request
            (maps/assoc-maybe :auth/user user)
            handler)))))

(defn with-headers
  "Ring middleware to allow header keys to be keywords."
  [_]
  (fn [handler]
    (fn [request]
      (-> request
          (maps/update-maybe :headers (partial maps/map-keys csk/->kebab-case-keyword))
          handler
          (maps/update-maybe :headers (partial maps/map-keys name))))))

(defn with-cors
  "Ring middleware to handle CORS headers in request/response."
  [_]
  (fn [handler]
    (fn [{:keys [headers] :as request}]
      (let [origin (:origin headers)
            req-headers (:access-control-request-headers headers)
            response-headers (cond-> {:access-control-allow-credentials "true"
                                      :access-control-allow-methods     "GET,POST,PUT,PATCH,DELETE,HEAD"}
                               origin (assoc :access-control-allow-origin origin)
                               req-headers (assoc :access-control-allow-headers (if (string? req-headers)
                                                                                  req-headers
                                                                                  (string/join "," req-headers))))]
        (if (= :options (:request-method request))
          (->response [::http/ok nil response-headers])
          (-> request
              handler
              (update :headers merge response-headers)))))))
