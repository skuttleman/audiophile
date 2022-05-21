(ns audiophile.common.infrastructure.http.impl
  (:require
    #?@(:clj [[clj-http.cookies :as cook]])
    [#?(:clj clj-http.client :cljs cljs-http.client) :as client]
    [clojure.core.async :as async]
    [clojure.set :as set]
    [clojure.string :as string]
    [audiophile.common.core.serdes.core :as serdes]
    [audiophile.common.core.serdes.impl :as serde]
    [audiophile.common.core.utils.logger :as log]
    [audiophile.common.core.utils.maps :as maps]
    [audiophile.common.infrastructure.http.core :as http]
    [audiophile.common.infrastructure.resources.protocols :as pres]
    [com.ben-allred.vow.core :as v #?@(:cljs [:include-macros true])]))

#?(:cljs
   (extend-protocol ICounted
     js/Blob
     (-count [this]
       (.-length this))))

(def ^:private ^:const timeout-msg
  {:error ^{:toast/msg     "Timeout - The request took longer than expected to complete."
            :http/timeout? true}
          [{:message "upload timed out"}]})

(def ^:const default-timeout 60000)

(defn ^:private find-serde
  ([headers serdes]
   (find-serde headers serdes nil))
  ([{:keys [accept content-type]} serdes default-mime-type]
   (serdes/find-serde serdes (or content-type accept default-mime-type))))

(defn ^:private deserialize* [{:keys [headers] :as response} serde serdes]
  (let [headers (maps/map-keys (comp keyword string/lower-case name) headers)
        serde (or (find-serde headers serdes) serde)]
    (-> response
        (update :status #(http/code->status % %))
        (update :body (comp #(cond->> %
                               (and serde (string? %)) (serdes/deserialize serde))
                            not-empty)))))

(defn ^:private with-headers*
  ([]
   (with-headers* nil))
  ([cs]
   (fn [result]
     (-> result
         #?(:clj (assoc :cookies (cook/get-cookies cs)))
         (update :headers (partial maps/map-keys keyword))))))

(defn ^:private response-logger [this request level ks]
  (fn [result]
    (log/with-ctx [this :HTTP#response]
      (log/log level (-> result (select-keys ks) (merge request))))))

(deftype HttpBase [http-client timeout]
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
                      (reject (ex-data ex)))))
                (async/go
                  (async/<! (async/timeout timeout))
                  (reject {:body timeout-msg}))))))

(deftype HttpLogger [http-client]
  pres/IResource
  (request! [this request]
    (let [req (select-keys request #{:method :url})]
      (log/with-ctx [this :HTTP#request]
        (log/info req))
      (v/peek (pres/request! http-client request)
              (response-logger this req :info #{:status})
              (response-logger this req :error #{:body :status})))))

(deftype HttpHeaders [http-client]
  pres/IResource
  (request! [_ request]
    (let [#?@(:clj [cs (cook/cookie-store)])
          result-fn (with-headers* #?(:clj cs))]
      (-> request
          (update :headers (partial maps/map-keys name))
          #?(:clj (assoc :cookie-store cs))
          (->> (pres/request! http-client))
          (v/then result-fn (comp v/reject result-fn))))))

(deftype HttpSerde [http-client serdes]
  pres/IResource
  (request! [_ request]
    (let [serde (find-serde (:headers request) serdes "application/transit")
          mime-type (serdes/mime-type serde)
          body (:body request)
          blob? (= :blob (:response-type request))
          deserialize (comp :body (if blob? identity #(deserialize* % serde serdes)))]
      (-> request
          (cond-> blob? (assoc-in [:headers :accept] "audio/mpeg"))
          (update :headers maps/assoc-defaults :accept mime-type)
          (cond->
            body
            (update :headers assoc :content-type mime-type)

            (and serde body)
            (update :body (partial serdes/serialize serde)))
          (->> (pres/request! http-client))
          (v/then deserialize (comp v/reject deserialize))))))

(deftype HttpProgress [http-client]
  pres/IResource
  (request! [_ request]
    (if-let [on-progress (:on-progress request)]
      (let [progress-ch (async/chan 100 (filter (comp #{:upload} :direction)))
            request (assoc request :progress progress-ch)]
        (async/go-loop []
          (when-let [{:keys [loaded total]} (async/<! progress-ch)]
            (on-progress {:progress/current loaded
                          :progress/total   total})
            (recur)))
        (-> http-client
            (pres/request! request)
            (v/peek (fn [[status]]
                      (on-progress {:progress/status status})
                      (async/close! progress-ch)))))
      (pres/request! http-client request))))

(defn client [{:keys [timeout]}]
  (reduce (fn [client middleware]
            (middleware client))
          (reify
            pres/IResource
            (request! [_ request]
              (-> request
                  (set/rename-keys {:params :query-params})
                  client/request)))
          [#(->HttpBase % (or timeout default-timeout))
           ->HttpLogger
           ->HttpHeaders
           #(->HttpSerde % serde/serdes)]))
