(ns com.ben-allred.audiophile.common.services.http
  (:require
    #?(:clj [clj-http.cookies :as cook])
    [#?(:clj clj-http.client :cljs cljs-http.client) :as client]
    [clojure.core.async :as async]
    [clojure.string :as string]
    [com.ben-allred.audiophile.common.services.navigation.core :as nav]
    [com.ben-allred.audiophile.common.services.resources.protocols :as pres]
    [com.ben-allred.audiophile.common.services.serdes.core :as serdes]
    [com.ben-allred.audiophile.common.utils.http :as http]
    [com.ben-allred.audiophile.common.utils.logger :as log]
    [com.ben-allred.audiophile.common.utils.maps :as maps]
    [com.ben-allred.vow.core :as v #?@(:cljs [:include-macros true])]
    [integrant.core :as ig]))

(defn ^:private ->serde [content-type serdes default-serde]
  (serdes/find-serde serdes content-type default-serde))

(defn ^:private find-serde [{:keys [accept content-type]} serdes]
  (serdes/find-serde serdes (or content-type accept "")))

(defn ^:private response*
  ([response?]
   (fn [value]
     (response* value response?)))
  ([value response?]
   (cond-> value (not response?) :body)))

(defn ^:private deserialize* [ch-response serde serdes]
  (let [{:keys [headers]
         :as   response} (-> (ex-data ch-response)
                             (or ch-response)
                             (update :headers (partial maps/map-keys keyword)))
        serde (try (find-serde headers serdes)
                   (catch #?(:cljs :default :default Throwable) _
                     serde))]
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

(defmethod ig/init-key ::base [_ {:keys [log-ctx]}]
  (fn [http-client]
    (reify
      pres/IResource
      (request! [_ request]
        (v/create (fn [resolve reject]
                    (async/go
                      (log/with-ctx (assoc log-ctx :sub :request)
                        (v/peek (try
                                  (log/info (select-keys request #{:method :url}))
                                  (let [val (-> request
                                                (->> (pres/request! http-client))
                                                #?@(:cljs [async/<! (as-> $ (or (ex-data $) $))]))]
                                    (if (http/success? val)
                                      (v/resolve val)
                                      (v/reject val)))
                                  (catch #?(:cljs :default :default Throwable) ex
                                    (v/reject (ex-data ex))))
                                (fn [result]
                                  (log/with-ctx {:sub :response}
                                    (log/info (-> request
                                                  (select-keys #{:method :url})
                                                  (assoc :status (:status result)))))
                                  (resolve result))
                                (fn [result]
                                  (log/with-ctx {:sub :response}
                                    (log/error (-> request
                                                   (select-keys #{:method :url})
                                                   (assoc :status (:status result)
                                                          :body (:body result)))))
                                  (reject result)))))))))))

(defmethod ig/init-key ::with-headers [_ _]
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

(defmethod ig/init-key ::with-serde [_ {:keys [serdes]}]
  (fn [http-client]
    (reify
      pres/IResource
      (request! [_ request]
        (let [serde (find-serde (:headers request) serdes)
              mime-type (serdes/mime-type serde)
              body (:body request)]
          (-> request
              (cond->
                (not mime-type)
                (update :headers maps/assoc-defaults :accept "application/edn")

                (and body mime-type)
                (update :headers (fn [headers]
                                   (-> headers
                                       (assoc :content-type mime-type)
                                       (maps/assoc-defaults :accept mime-type))))

                (and serde body)
                (update :body (partial serdes/serialize serde)))
              (->> (pres/request! http-client))
              (v/then-> (deserialize* serde serdes))
              (v/then (response* (:response? request))
                      (comp v/reject
                            (response* (:response? request))))))))))

(defmethod ig/init-key ::with-nav [_ {:keys [nav]}]
  (fn [http-client]
    (reify
      pres/IResource
      (request! [_ {:nav/keys [route params] :as request}]
        (-> request
            (dissoc :nav/route :nav/params)
            (cond-> route (assoc :url (nav/path-for nav route params)))
            (->> (pres/request! http-client)))))))

(defmethod ig/init-key ::client [_ {:keys [middlewares]}]
  (reduce (fn [client middleware]
            (middleware client))
          (reify
            pres/IResource
            (request! [_ request]
              (client/request request)))
          middlewares))

(defmethod ig/init-key ::stub [_ {:keys [result result-fn]}]
  (reify
    pres/IResource
    (request! [_ request]
      (v/resolve (if result-fn
                   (result-fn request)
                   result)))))
