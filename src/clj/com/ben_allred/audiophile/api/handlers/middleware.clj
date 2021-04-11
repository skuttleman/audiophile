(ns com.ben-allred.audiophile.api.handlers.middleware
  (:require
    [camel-snake-kebab.core :as csk]
    [clojure.core.match :as match]
    [clojure.string :as string]
    [com.ben-allred.audiophile.api.services.serdes.core :as serdes]
    [com.ben-allred.audiophile.common.services.http :as http]
    [com.ben-allred.audiophile.common.services.navigation :as nav]
    [com.ben-allred.audiophile.common.utils.logger :as log]
    [com.ben-allred.audiophile.common.utils.maps :as maps]
    [integrant.core :as ig])
  (:import
    (java.io File)))

(defmethod ig/init-key ::vector-response [_ _]
  (fn [handler]
    (fn [request]
      (match/match (handler request)
        nil {:status (http/status->code :http.status/not-found)}
        [status] {:status (http/status->code status)}
        [status body] {:status (http/status->code status) :body body}
        [status body headers] {:status (http/status->code status) :body body :headers headers}
        response response))))

(defmethod ig/init-key ::serde [_ {:keys [edn-serde transit-serde]}]
  (fn [handler]
    (fn [request]
      (let [serde (condp string/starts-with? (or (get-in request [:headers "accept"])
                                                 (get-in request [:headers "content-type"])
                                                 "")
                    (serdes/mime-type transit-serde) transit-serde
                    edn-serde)
            {resp :body :as response} (-> request
                                          (maps/update-maybe :body (partial serdes/deserialize serde))
                                          handler)
            serde' (condp string/starts-with? (or (get-in response [:headers "Accept"])
                                                  (get-in response [:headers "Content-Type"])
                                                  "")
                     (serdes/mime-type edn-serde) edn-serde
                     (serdes/mime-type transit-serde) transit-serde
                     serde)]
        (cond-> response
          (and (some? resp)
               (not (instance? File resp))
               (not (string? resp)))
          (-> (update :body (partial serdes/serialize serde'))
              (update-in [:headers "Content-Type"] #(or % (serdes/mime-type serde')))))))))

(defmethod ig/init-key ::with-route [_ _]
  (fn [handler]
    (fn [{:keys [query-string uri] :as request}]
      (-> request
          (assoc :nav/route (nav/match-route (cond-> uri
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
              [time unit] (cond
                            (> elapsed 1000000) [(long (/ elapsed 1000000)) "m"]
                            (> elapsed 1000) [(long (/ elapsed 1000)) "μ"]
                            :else [elapsed "n"])]
          (log/info (format "%s %s [%d%ss] - %d"
                            (csk/->SCREAMING_SNAKE_CASE_STRING (:request-method request))
                            (:uri request)
                            time
                            unit
                            (:status response)))
          (log/spy :debug (:body response))
          response)
        (handler request)))))
