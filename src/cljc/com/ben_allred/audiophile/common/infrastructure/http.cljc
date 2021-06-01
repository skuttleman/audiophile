(ns com.ben-allred.audiophile.common.infrastructure.http
  (:require
    #?(:clj [clj-http.cookies :as cook])
    [#?(:clj clj-http.client :cljs cljs-http.client) :as client]
    [clojure.core.async :as async]
    [com.ben-allred.audiophile.common.app.navigation.core :as nav]
    [com.ben-allred.audiophile.common.app.resources.protocols :as pres]
    [com.ben-allred.audiophile.common.core.serdes.core :as serdes]
    [com.ben-allred.audiophile.common.app.http :as http]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.common.core.utils.maps :as maps]
    [com.ben-allred.vow.core :as v #?@(:cljs [:include-macros true])]))

(defn ^:private find-serde
  ([headers serdes]
   (find-serde headers serdes nil))
  ([{:keys [accept content-type]} serdes default-mime-type]
   (serdes/find-serde serdes (or content-type accept default-mime-type ""))))

(defn ^:private response*
  ([response?]
   (fn [value]
     (response* value response?)))
  ([value response?]
   (cond-> value (not response?) :body)))

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

(defn with-logging [{:keys [log-ctx]}]
  (fn [http-client]
    (reify
      pres/IResource
      (request! [_ request]
        (log/with-ctx (assoc log-ctx :sub :request)
          (log/info (select-keys request #{:method :url}))
          (v/peek (pres/request! http-client request)
                  (fn [result]
                    (log/with-ctx {:sub :response}
                      (log/info (-> request
                                    (select-keys #{:method :url})
                                    (assoc :status (:status result))))))
                  (fn [result]
                    (log/with-ctx {:sub :response}
                      (log/error (-> request
                                     (select-keys #{:method :url})
                                     (assoc :status (:status result)
                                            :body (:body result))))))))))))

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
              deserde #(deserialize* % serde serdes)]
          (-> request
              (update :headers maps/assoc-defaults :accept mime-type)
              (cond->
                body
                (update :headers assoc :content-type mime-type)

                (and serde body)
                (update :body (partial serdes/serialize serde)))
              (->> (pres/request! http-client))
              (v/then (comp (response* (:response? request)) deserde)
                      (comp v/reject
                            (response* (:response? request))
                            deserde))))))))

(defn with-nav [{:keys [nav]}]
  (fn [http-client]
    (reify
      pres/IResource
      (request! [_ {:nav/keys [route params] :as request}]
        (-> request
            (dissoc :nav/route :nav/params)
            (cond-> route (assoc :url (nav/path-for nav route params)))
            (->> (pres/request! http-client)))))))

(defn create [->request middlewares]
  (reduce (fn [client middleware]
            (middleware client))
          (reify
            pres/IResource
            (request! [_ request]
              (->request request)))
          middlewares))

(defn client [{:keys [middlewares]}]
  (create client/request middlewares))

(defn stub [{:keys [result result-fn]}]
  (reify
    pres/IResource
    (request! [_ request]
      (v/resolve (if result-fn
                   (result-fn request)
                   result)))))
