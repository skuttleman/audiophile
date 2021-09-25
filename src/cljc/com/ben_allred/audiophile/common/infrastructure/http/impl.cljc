(ns com.ben-allred.audiophile.common.infrastructure.http.impl
  (:require
    #?(:clj [clj-http.cookies :as cook])
    [#?(:clj clj-http.client :cljs cljs-http.client) :as client]
    [clojure.core.async :as async]
    [com.ben-allred.audiophile.common.api.navigation.core :as nav]
    [com.ben-allred.audiophile.common.core.resources.protocols :as pres]
    [com.ben-allred.audiophile.common.core.serdes.core :as serdes]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.common.core.utils.maps :as maps]
    [com.ben-allred.audiophile.common.core.utils.uuids :as uuids]
    [com.ben-allred.audiophile.common.infrastructure.http.core :as http]
    [com.ben-allred.audiophile.common.infrastructure.pubsub.core :as pubsub]
    [com.ben-allred.vow.core :as v #?@(:cljs [:include-macros true])]))

(defn ^:private find-serde
  ([headers serdes]
   (find-serde headers serdes nil))
  ([{:keys [accept content-type]} serdes default-mime-type]
   (serdes/find-serde serdes (or content-type accept default-mime-type ""))))

(defn ^:private deserialize* [ch-response serde serdes]
  (let [{:keys [headers] :as response} (update ch-response
                                               :headers
                                               (partial maps/map-keys keyword))
        serde (or (find-serde headers serdes) serde)]
    (-> response
        (update :status #(http/code->status % %))
        (update :body #(cond->> %
                                (and serde (string? %)) (serdes/deserialize serde))))))

(defn ^:private with-headers*
  ([]
   (with-headers* nil))
  ([cs]
   (fn [result]
     (-> result
         #?(:clj (assoc :cookies (cook/get-cookies cs)))
         (update :headers (partial maps/map-keys keyword))))))

(defn base [_]
  (fn [http-client]
    (reify
      pres/IResource
      (request! [_ request]
        (v/create (fn [resolve reject]
                    (async/go
                      (try
                        (let [val (-> request
                                      (->> (pres/request! http-client))
                                      #?@(:cljs [async/<! (as-> $ (or (ex-data $) $))]))]
                          (if (http/success? val)
                            (resolve val)
                            (reject val)))
                        (catch #?(:cljs :default :default Throwable) ex
                          (reject (ex-data ex)))))))))))

(defn ^:private response-logger [request level ks]
  (fn [result]
    (log/with-ctx {:sub :response}
      (log/log level
               "[Response]"
               (-> result
                   (select-keys ks)
                   (merge request))))))

(defn with-logging [{:keys [log-ctx]}]
  (fn [http-client]
    (reify
      pres/IResource
      (request! [_ request]
        (log/with-ctx (assoc log-ctx :sub :request)
          (let [req (select-keys request #{:method :url})]
            (log/info "[Request]" req)
            (v/peek (pres/request! http-client request)
                    (response-logger req :info #{:status})
                    (response-logger req :error #{:body :status}))))))))

(defn with-headers [_]
  (fn [http-client]
    (reify
      pres/IResource
      (request! [_ request]
        (let [#?@(:clj [cs (cook/cookie-store)])
              result-fn (with-headers* #?(:clj cs))]
          (-> request
              (update :headers (partial maps/map-keys name))
              #?(:clj (assoc :cookie-store cs))
              (->> (pres/request! http-client))
              (v/then result-fn (comp v/reject result-fn))))))))

(defn with-serde [{:keys [serdes]}]
  (fn [http-client]
    (reify
      pres/IResource
      (request! [_ request]
        (let [serde (find-serde (:headers request) serdes "application/edn")
              mime-type (serdes/mime-type serde)
              body (:body request)
              deserde (comp :body #(deserialize* % serde serdes))]
          (-> request
              (update :headers maps/assoc-defaults :accept mime-type)
              (cond->
                body
                (update :headers assoc :content-type mime-type)

                (and serde body)
                (update :body (partial serdes/serialize serde)))
              (->> (pres/request! http-client))
              (v/then deserde (comp v/reject deserde))))))))

(defn with-nav [{:keys [nav]}]
  (fn [http-client]
    (reify
      pres/IResource
      (request! [_ {:nav/keys [route params] :as request}]
        (-> request
            (dissoc :nav/route :nav/params)
            (cond-> route (assoc :url (nav/path-for nav route params)))
            (->> (pres/request! http-client)))))))

(defn ^:private with-pubsub* [http-client pubsub ms request]
  (let [pubsub-id (uuids/random)]
    (-> (v/create (fn [resolve reject]
                    (let [request-id (uuids/random)]
                      (pubsub/subscribe! pubsub pubsub-id request-id (fn [_ event]
                                                                       (if (:error event)
                                                                         (reject event)
                                                                         (resolve event))))
                      (-> http-client
                          (pres/request! (assoc-in request [:headers :x-request-id] request-id))
                          (v/catch reject))
                      (v/and (v/sleep ms)
                             (reject {:error ^{:toast/msg "Timeout - The request took longer than expected to complete"}
                                             [{:message "upload timed out"}]})))))
        (v/peek (fn [_]
                  (pubsub/unsubscribe! pubsub pubsub-id))))))

(defn with-pubsub [{:keys [pubsub timeout]}]
  (let [timeout (or timeout 30000)]
    (fn [http-client]
      (reify
        pres/IResource
        (request! [_ request]
          (if (:http/async? request)
            (with-pubsub* http-client pubsub timeout request)
            (pres/request! http-client request)))))))

(defn create [respond-fn middlewares]
  (reduce (fn [client middleware]
            (middleware client))
          (reify
            pres/IResource
            (request! [_ request]
              (respond-fn request)))
          middlewares))

(defn client [{:keys [middlewares]}]
  (create client/request middlewares))
