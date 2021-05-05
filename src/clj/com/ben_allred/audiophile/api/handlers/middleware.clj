(ns com.ben-allred.audiophile.api.handlers.middleware
  (:require
    [camel-snake-kebab.core :as csk]
    [clojure.core.match :as match]
    [clojure.string :as string]
    [com.ben-allred.audiophile.common.services.navigation.core :as nav]
    [com.ben-allred.audiophile.common.services.serdes.core :as serdes]
    [com.ben-allred.audiophile.common.utils.http :as http]
    [com.ben-allred.audiophile.common.utils.logger :as log]
    [com.ben-allred.audiophile.common.utils.maps :as maps]
    [integrant.core :as ig])
  (:import
    (clojure.lang ExceptionInfo)
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
       (empty? (for [class [File InputStream Channel String]
                     :when (instance? class resp)]
                 class))))

(defmethod ig/init-key ::vector-response [_ _]
  (fn [handler]
    (fn [request]
      (-> request handler ->response))))

(defmethod ig/init-key ::serde [_ serdes]
  (fn [handler]
    (fn [request]
      (let [serde (serdes/find-serde serdes
                                     (or (get-in request [:headers :accept])
                                         (get-in request [:headers :content-type])
                                         "application/edn"))
            {resp :body :as response} (-> request
                                          (maps/update-maybe :body (partial serdes/deserialize serde))
                                          handler)
            serde' (serdes/find-serde serdes
                                      (or (get-in response [:headers :accept])
                                          (get-in response [:headers :content-type])
                                          "unknown/mime-type")
                                      serde)]
        (cond-> response
          (serializable? resp)
          (-> (update :body (partial serdes/serialize serde'))
              (update-in [:headers :content-type] #(or % (serdes/mime-type serde')))))))))

(defmethod ig/init-key ::with-route [_ {:keys [nav]}]
  (fn [handler]
    (fn [{:keys [query-string uri] :as request}]
      (-> request
          (assoc :nav/route (nav/match-route nav
                                             (cond-> uri
                                               query-string (str "?" query-string))))
          handler))))

(defmethod ig/init-key ::with-logging [_ _]
  (fn [handler]
    (fn [request]
      (if (some-> request
                  (get-in [:nav/route :handler])
                  ((some-fn #{:resources/health}
                            (comp #{"auth" "api"} namespace))))
        (let [start (System/nanoTime)
              response (handler request)
              end (System/nanoTime)
              elapsed (- end start)
              color (cond
                      (>= elapsed 1000000000) "\u001B[31m"
                      (>= elapsed 100000000) "\u001B[33;1m"
                      (>= elapsed 1000000) "\u001b[37;1m"
                      (>= elapsed 1000) "\u001b[36m"
                      :else "\u001b[32;1m")
              time (cond
                     (>= elapsed 1000000000) (str color (long (/ elapsed 1000000000)) "s\u001B[0m")
                     (>= elapsed 1000000) (str color (long (/ elapsed 1000000)) "ms\u001B[0m")
                     (>= elapsed 1000) (str color (long (/ elapsed 1000)) "μs\u001B[0m")
                     :else (str color elapsed "ns\u001B[0m"))
              msg (format "%s %s [%s] - %d"
                          (csk/->SCREAMING_SNAKE_CASE_STRING (:request-method request))
                          (:uri request)
                          time
                          (:status response))]
          (if (::ex (meta response))
            (log/warn msg)
            (log/info msg))
          response)
        (handler request)))))

(defmethod ig/init-key ::with-auth [_ {:keys [jwt-serde]}]
  "parses the auth-token on the request"
  (fn [handler]
    (fn [request]
      (let [jwt (get-in request [:cookies "auth-token" :value])
            user (serdes/deserialize jwt-serde jwt)]
        (-> request
            (maps/assoc-maybe :auth/user user)
            handler)))))

(defmethod ig/init-key ::with-ex [_ {:keys [err-msg]}]
  "catches exceptions thrown from a handler and produces an error response"
  (let [err-response ^::ex {:status 500 :body {:errors [{:message (or err-msg "an unexpected error occurred")}]}}]
    (fn [handler]
      (fn [request]
        (try (handler request)
             (catch ExceptionInfo ex
               (log/error ex)
               (if-let [response (:response (ex-data ex))]
                 (vary-meta (->response response) assoc ::ex true)
                 err-response))
             (catch Throwable ex
               (log/error ex)
               err-response))))))

(defmethod ig/init-key ::with-headers [_ _]
  (fn [handler]
    (fn [request]
      (-> request
          (maps/update-maybe :headers (partial maps/map-keys csk/->kebab-case-keyword))
          handler
          (maps/update-maybe :headers (partial maps/map-keys name))))))
