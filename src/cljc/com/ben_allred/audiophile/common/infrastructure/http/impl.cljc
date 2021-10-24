(ns com.ben-allred.audiophile.common.infrastructure.http.impl
  (:require
    #?@(:clj  [[clj-http.cookies :as cook]]
        :cljs [[com.ben-allred.audiophile.ui.core.components.core :as comp]
               [com.ben-allred.audiophile.ui.core.utils.dom :as dom]])
    [#?(:clj clj-http.client :cljs cljs-http.client) :as client]
    [clojure.core.async :as async]
    [clojure.set :as set]
    [com.ben-allred.audiophile.common.api.navigation.core :as nav]
    [com.ben-allred.audiophile.common.api.pubsub.core :as pubsub]
    [com.ben-allred.audiophile.common.core.resources.protocols :as pres]
    [com.ben-allred.audiophile.common.core.serdes.core :as serdes]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.common.core.utils.maps :as maps]
    [com.ben-allred.audiophile.common.core.utils.uuids :as uuids]
    [com.ben-allred.audiophile.common.infrastructure.http.core :as http]
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

(def ^:private ^:const default-timeout 60000)

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

(defn base [{:keys [timeout]}]
  (let [timeout (or timeout default-timeout)]
    (fn [http-client]
      (->HttpBase http-client timeout))))

(defn ^:private ->auth-handler [*banners nav]
  #?(:cljs    (let [banner {:level :error
                            :key   ::unauthenticated
                            :body  [:div
                                    "Your session has expired. Please "
                                    [:a
                                     {:href     "#"
                                      :on-click (fn [e]
                                                  (dom/stop-propagation e)
                                                  (nav/goto! nav :auth/logout))}
                                     "login"]
                                    "."]}]
                (fn [err]
                  (when (= ::http/unauthorized (-> err :status http/code->status))
                    (comp/create! *banners banner))
                  (v/reject err)))
     :default v/reject))

(deftype HttpAuth [http-client auth-handler]
  pres/IResource
  (request! [_ request]
    (-> (pres/request! http-client request)
        (v/catch auth-handler))))

(defn with-unauthorized [{:keys [*banners nav]}]
  (fn [http-client]
    (->HttpAuth http-client (->auth-handler *banners nav))))

(defn ^:private response-logger [this request level ks]
  (fn [result]
    (log/with-ctx [this :HTTP#response]
      (log/log level (-> result (select-keys ks) (merge request))))))

(deftype HttpLogger [http-client]
  pres/IResource
  (request! [this request]
    (let [req (select-keys request #{:method :url})]
      (log/with-ctx [this :HTTP#request]
        (log/info req))
      (v/peek (pres/request! http-client request)
              (response-logger this req :info #{:status})
              (response-logger this req :error #{:body :status})))))

(defn with-logging [_]
  ->HttpLogger)

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

(defn with-headers [_]
  ->HttpHeaders)

(deftype HttpSerde [http-client serdes]
  pres/IResource
  (request! [_ request]
    (let [serde (find-serde (:headers request) serdes "application/edn")
          mime-type (serdes/mime-type serde)
          body (:body request)
          blob? (= :blob (:response-type request))
          deserde (comp :body (if blob? identity #(deserialize* % serde serdes)))]
      (-> request
          (cond-> blob? (assoc-in [:headers :accept] "audio/mpeg"))
          (update :headers maps/assoc-defaults :accept mime-type)
          (cond->
            body
            (update :headers assoc :content-type mime-type)

            (and serde body)
            (update :body (partial serdes/serialize serde)))
          (->> (pres/request! http-client))
          (v/then deserde (comp v/reject deserde))))))

(defn with-serde [{:keys [serdes]}]
  (fn [http-client]
    (->HttpSerde http-client serdes)))

(deftype HttpNav [http-client nav route->event-type]
  pres/IResource
  (request! [_ {:nav/keys [route params] :as request}]
    (-> request
        (dissoc :nav/route :nav/params)
        (update :async/event-types #(or % (route->event-type route)))
        (cond-> route (assoc :url (nav/path-for nav route params)))
        (->> (pres/request! http-client)))))

(defn with-nav [{:keys [nav route->event-type]}]
  (let [route->event-type (or route->event-type
                              {:api/artifacts     #{:artifact/created}
                               :api/comments      #{:comment/created}
                               :api/file          #{:file-version/created}
                               :api/project.files #{:file/created}
                               :api/projects      #{:project/created}
                               :api/teams         #{:team/created}
                               :api/users         #{:user/created :team/created}})]
    (fn [http-client]
      (->HttpNav http-client nav route->event-type))))

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

(defn with-progress [_]
  ->HttpProgress)

(defn ^:private with-async* [http-client pubsub ms {{request-id :x-request-id} :headers :as request}]
  (let [pubsub-id (uuids/random)
        sub-events (:async/event-types request)
        results (atom {})]
    (-> (v/create (fn [resolve reject]
                    (letfn [(handler [_ {event-type :event/type :as event}]
                              (if (= :command/failed event-type)
                                (reject event)
                                (let [results (swap! results assoc event-type event)]
                                  (when (every? results sub-events)
                                    (resolve {:data (transduce (map :data) into {} (vals results))})))))]
                      (pubsub/subscribe! pubsub pubsub-id request-id handler)
                      (-> http-client
                          (pres/request! request)
                          (v/catch reject))
                      (v/and (v/sleep ms)
                             (reject timeout-msg)))))
        (v/peek (fn [_]
                  (pubsub/unsubscribe! pubsub pubsub-id))))))

(deftype HttpAsync [http-client pubsub timeout]
  pres/IResource
  (request! [_ request]
    (let [request (assoc-in request [:headers :x-request-id] (uuids/random))]
      (if (:http/async? request)
        (with-async* http-client pubsub timeout request)
        (pres/request! http-client request)))))

(defn with-async [{:keys [pubsub timeout]}]
  (let [timeout (or timeout default-timeout)]
    (fn [http-client]
      (->HttpAsync http-client pubsub timeout))))

(defn create [respond-fn middlewares]
  (reduce (fn [client middleware]
            (middleware client))
          (reify
            pres/IResource
            (request! [_ request]
              (respond-fn request)))
          middlewares))

(defn client [{:keys [middlewares]}]
  (create (comp client/request
                #(set/rename-keys % {:params :query-params}))
          middlewares))
