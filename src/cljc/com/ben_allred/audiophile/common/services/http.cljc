(ns com.ben-allred.audiophile.common.services.http
  (:refer-clojure :exclude [get])
  (:require
    #?(:clj [clj-http.cookies :as cook])
    [#?(:clj clj-http.client :cljs cljs-http.client) :as client]
    [#?(:clj clj-http.core :cljs cljs-http.core) :as http*]
    [clojure.core.async :as async]
    [com.ben-allred.audiophile.common.services.serdes.core :as serdes]
    [clojure.set :as set]
    [medley.core :as medley]
    [com.ben-allred.vow.core :as v]
    [com.ben-allred.audiophile.common.utils.maps :as maps]
    [integrant.core :as ig]))

(defprotocol IHttpClient
  (request! [this opts]))

(def code->status
  {200 :http.status/ok
   201 :http.status/created
   202 :http.status/accepted
   203 :http.status/non-authoritative-information
   204 :http.status/no-content
   205 :http.status/reset-content
   206 :http.status/partial-content
   300 :http.status/multiple-choices
   301 :http.status/moved-permanently
   302 :http.status/found
   303 :http.status/see-other
   304 :http.status/not-modified
   305 :http.status/use-proxy
   306 :http.status/unused
   307 :http.status/temporary-redirect
   400 :http.status/bad-request
   401 :http.status/unauthorized
   402 :http.status/payment-required
   403 :http.status/forbidden
   404 :http.status/not-found
   405 :http.status/method-not-allowed
   406 :http.status/not-acceptable
   407 :http.status/proxy-authentication-required
   408 :http.status/request-timeout
   409 :http.status/conflict
   410 :http.status/gone
   411 :http.status/length-required
   412 :http.status/precondition-failed
   413 :http.status/request-entity-too-large
   414 :http.status/request-uri-too-long
   415 :http.status/unsupported-media-type
   416 :http.status/requested-range-not-satisfiable
   417 :http.status/expectation-failed
   500 :http.status/internal-server-error
   501 :http.status/not-implemented
   502 :http.status/bad-gateway
   503 :http.status/service-unavailable
   504 :http.status/gateway-timeout
   505 :http.status/http-version-not-supported})

(def status->code
  (set/map-invert code->status))

(defn ^:private check-status [lower upper response]
  (let [status (or (when (vector? response)
                     (status->code (first response)))
                   (as-> response $
                         (cond-> $ (vector? $) second)
                         (:status $)
                         (cond-> $ (keyword? $) status->code)))]
    (<= lower status upper)))

(def ^{:arglists '([response])} success?
  (partial check-status 200 299))

(def ^{:arglists '([response])} client-error?
  (partial check-status 400 499))

(def ^{:arglists '([response])} server-error?
  (partial check-status 500 599))

(defn ^:private client* [opts]
  (let [client* (-> http*/request
                    client/wrap-query-params
                    client/wrap-basic-auth
                    client/wrap-oauth
                    client/wrap-url
                    client/wrap-accept
                    client/wrap-content-type
                    client/wrap-form-params
                    client/wrap-method
                    #?@(:clj  [client/wrap-request-timing
                               client/wrap-decompression
                               client/wrap-input-coercion
                               client/wrap-user-info
                               client/wrap-additional-header-parsing
                               client/wrap-output-coercion
                               client/wrap-exceptions
                               client/wrap-nested-params
                               client/wrap-accept-encoding
                               client/wrap-flatten-nested-params
                               client/wrap-unknown-host]
                        :cljs [client/wrap-multipart-params
                               client/wrap-channel-from-request-map]))]
    #?(:clj  (let [cs (cook/cookie-store)
                   ch (async/chan)]
               (-> opts
                   (update :headers (partial medley/map-keys name))
                   (merge {:async? true :cookie-store cs})
                   (client* (fn [response]
                              (async/put! ch (assoc response :cookies (cook/get-cookies cs))))
                            (fn [exception]
                              (async/put! ch (assoc (ex-data exception) :cookies (cook/get-cookies cs))))))
               ch)
       :cljs (-> opts
                 (update :headers (partial medley/map-keys name))
                 client*))))

(defn ->serde [content-type serdes default-serde]
  (loop [[[_ serde] :as serdes] (seq serdes)]
    (cond
      (empty? serdes) default-serde
      (= content-type (serdes/mime-type serde)) serde
      :else (recur (rest serdes)))))

(defn ^:private request* [ch serdes]
  (async/go
    (let [ch-response (async/<! ch)
          {:keys [headers] :as response} (-> (if-let [data (ex-data ch-response)]
                                               data
                                               ch-response)
                                             (update :headers (partial medley/map-keys keyword)))
          serde (->serde (:content-type headers) serdes (:edn serdes))]
      (-> response
          (update :status #(code->status % %))
          (update :body #(cond->> %
                                  (and serde (string? %)) (serdes/deserialize serde)))
          (->> (conj [(if (success? response) :success :error)]))))))

(defn ^:private response*
  ([response?]
   (fn [value]
     (response* value response?)))
  ([value response?]
   (cond-> value (not response?) :body)))

(defmethod ig/init-key ::client [_ {:keys [serdes]}]
  (reify IHttpClient
    (request! [_ opts]
      (let [content-type (if (:dev? opts)
                           (serdes/mime-type (:edn serdes))
                           (serdes/mime-type (:transit serdes)))
            headers (merge {:content-type content-type
                            :accept       content-type}
                           (:headers opts))
            serde (->serde (:content-type headers) serdes (:edn serdes))]
        (-> opts
            (maps/update-maybe :body (partial serdes/serialize serde))
            (update :headers merge headers)
            client*
            (request* serdes)
            (v/ch->prom (comp #{:success} first))
            (v/then (comp (response* (:response? opts))
                          second)
                    (comp v/reject
                          (response* (:response? opts))
                          second)))))))

(defmethod ig/init-key ::stub [_ {:keys [result result-fn]}]
  (reify
    IHttpClient
    (request! [_ opts]
      (v/resolve (if result-fn
                   (result-fn opts)
                   result)))))

(defn get
  ([client url]
   (get client url nil))
  ([client url request]
   (request! client (assoc request :method :get :url url))))

(defn post [client url request]
  (request! client (assoc request :method :post :url url)))

(defn patch [client url request]
  (request! client (assoc request :method :patch :url url)))

(defn put [client url request]
  (request! client (assoc request :method :put :url url)))

(defn delete
  ([client url]
   (delete client url nil))
  ([client url request]
   (request! client (assoc request :method :delete :url url))))
