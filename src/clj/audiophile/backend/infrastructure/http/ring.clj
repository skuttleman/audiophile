(ns audiophile.backend.infrastructure.http.ring
  (:require
    [camel-snake-kebab.core :as csk]
    [clojure.string :as string]
    [audiophile.common.core.utils.logger :as log]
    [audiophile.common.core.utils.maps :as maps]
    [audiophile.common.core.utils.uuids :as uuids]
    [ring.middleware.cookies :as ring.cook]
    [ring.middleware.multipart-params :as ring.multi]
    [ring.middleware.resource :as ring.res]
    [ring.util.response :as ring.resp])
  (:import
    (clojure.lang ExceptionInfo)
    (org.apache.commons.fileupload.util LimitedInputStream)
    (java.io InputStream)))

(def ^{:arglists '([url])} redirect
  "Produce a ring-style response to redirect to a url"
  ring.resp/redirect)

(def ^{:arglists '([request root-path])} resource-request
  "A handler function that will respond with static assets when they exist in the resources folder"
  ring.res/resource-request)

(def ^{:arglists '([handler] [handler options])} wrap-cookies
  "Ring middleware for encoding and decoding cookies"
  ring.cook/wrap-cookies)

(def ^{:arglists '([handler] [handler options])} wrap-multipart-params
  ring.multi/wrap-multipart-params)

(defn limit [{:keys [max-size]}]
  (fn [handler]
    (fn [request]
      (try (-> request
               (update :body (fn [stream]
                               (if (instance? InputStream stream)
                                 (proxy [LimitedInputStream] [stream max-size]
                                   (raiseError [_ processed]
                                     (log/error "request exceeds max length" (get-in request [:headers :x-request-id]))
                                     (throw (ex-info "max size exceeded"
                                                     (maps/->m {::limit? true} max-size processed))))))
                               stream))
               handler)
           (catch ExceptionInfo ex
             (if (::limit? (ex-data ex))
               {:status 413}
               (throw ex)))))))


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
  (let [time (log-time elapsed)
        query (if-let [qs (:query-string request)]
                (str "?" qs)
                "")]
    (format "%s %s%s [%s] - %d"
            (csk/->SCREAMING_SNAKE_CASE_STRING (:request-method request))
            (:uri request)
            query
            time
            (:status response))))

(defn with-logging
  "Ring middleware that logs response statistics."
  [handler]
  (fn [{:keys [uri] :as request}]
    (log/with-ctx {:request/id (-> request
                                   (get-in [:headers "x-request-id"])
                                   uuids/->uuid)
                   :logger/id  :SERVER}
      (if (or (string/starts-with? uri "/js")
              (string/starts-with? uri "/css"))
        (handler request)
        (let [start (System/nanoTime)
              response (handler request)
              end (System/nanoTime)
              elapsed (- end start)
              msg (log-msg request response elapsed)]
          (if (::ex (meta response))
            (log/warn msg)
            (log/info msg))
          response)))))

(defn ->cookie
  "Creates a ring-style cookie map suitable to be encoded by ring"
  ([k v]
   (->cookie k v nil))
  ([k v params]
   {k (maps/assoc-defaults params :value v :http-only true :path "/")}))

(defn error
  "Formats an error msg as an error response map"
  [status msg]
  [status {:error {:errors [{:message msg}]}}])
