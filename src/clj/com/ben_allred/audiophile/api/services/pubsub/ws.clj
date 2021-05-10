(ns com.ben-allred.audiophile.api.services.pubsub.ws
  (:require
    [clojure.core.async :as async]
    [com.ben-allred.audiophile.api.services.pubsub.protocols :as pws]
    [com.ben-allred.audiophile.common.services.pubsub.core :as pubsub]
    [com.ben-allred.audiophile.common.services.serdes.core :as serdes]
    [com.ben-allred.audiophile.common.utils.core :as u]
    [com.ben-allred.audiophile.common.utils.logger :as log]
    [com.ben-allred.audiophile.common.utils.uuids :as uuids]
    [immutant.web.async :as web.async]
    [integrant.core :as ig]))

(defmulti ^:private handle-msg (fn [_ _ event]
                                 (log/debug "received event" event)
                                 (when (seqable? event)
                                   (first event))))
(defmethod handle-msg :default
  [_ _ event]
  (log/warn "event not handled" event))

(defmethod handle-msg :conn/ping
  [_ channel _]
  (pws/send! channel [:conn/pong]))

(defmethod handle-msg :conn/pong
  [_ _ _])

(defn ^:private subscribe* [{:keys [ch-id pubsub]} channel topic ->event]
  (pubsub/subscribe! pubsub
                     ch-id
                     topic
                     (fn [_ [event-id event]]
                       (pws/send! channel (->event event-id event)))))

(defn ^:private ch-loop [{:keys [heartbeat-int-ms]} channel]
  (async/go-loop []
    (async/<! (async/timeout heartbeat-int-ms))
    (when (pws/open? channel)
      (try
        (pws/send! channel [:conn/ping])
        (catch Throwable _
          (pws/close! channel)))
      (recur))))

(defn ^:private handle-open [{:keys [user-id] :as ctx} channel]
  (subscribe* ctx channel [::broadcast] (partial vector :event/broadcast))
  (subscribe* ctx channel [::user user-id] #(vector :event/user %1 %2 {:user/id user-id}))
  (ch-loop ctx channel))

(defn ^:private handle-close [{:keys [ch-id pubsub user-id]} _]
  (log/info "websocket closed:" ch-id user-id)
  (pubsub/unsubscribe! pubsub ch-id))

(defmethod ig/init-key ::->handler [_ {:keys [heartbeat-int-ms pubsub serdes]}]
  (fn [request channel]
    (let [user-id (get-in request [:auth/user :user/id])
          params (get-in request [:nav/route :query-params])
          deserializer (serdes/find-serde serdes
                                          (or (:content-type params)
                                              (:accept params)))
          ctx {:ch-id            (uuids/random)
               :heartbeat-int-ms heartbeat-int-ms
               :user-id          user-id
               :pubsub           pubsub
               :state            (ref nil)}]
      (reify
        pws/IHandler
        (on-open [_]
          (handle-open ctx channel))
        (on-message [_ msg]
          (handle-msg ctx channel (cond->> msg
                                    deserializer (serdes/deserialize deserializer))))
        (on-close [_]
          (handle-close ctx channel))))))

(defn ^:private send* [channel serde msg]
  (try
    (pws/send! channel
               (cond->> msg
                 serde (serdes/serialize serde)))
    (catch Throwable ex
      (log/warn ex "failed to send msg to websocket" msg)
      (throw ex))))

(defn ^:private close* [channel]
  (try
    (pws/close! channel)
    (catch Throwable ex
      (log/warn ex "web socket did not close successfully"))))

(deftype SerdeChannel [channel serializer]
  pws/IChannel
  (open? [_]
    (pws/open? channel))
  (send! [_ msg]
    (send* channel serializer msg))
  (close! [_]
    (close* channel)))

(defmethod ig/init-key ::->channel [_ {:keys [serdes]}]
  (fn [request channel]
    (let [params (get-in request [:nav/route :query-params])
          serializer (serdes/find-serde! serdes
                                         (or (:accept params)
                                             (:content-type params)))]
      (->SerdeChannel channel serializer))))

(deftype Channel [ch]
  pws/IChannel
  (open? [_]
    (web.async/open? ch))
  (send! [_ msg]
    (web.async/send! ch msg))
  (close! [_]
    (web.async/close ch)))

(defn ^:private ->handler-fn [request {:keys [->channel ->handler]}]
  (let [handler (promise)]
    (fn [ch]
      (when-not (realized? handler)
        (->> (->Channel ch)
             (->channel request)
             (->handler request)
             (deliver handler)))
      @handler)))

(defn ch-map [->handler]
  {:on-open    (comp pws/on-open ->handler)
   :on-message (fn [ch msg]
                 (pws/on-message (->handler ch) msg))
   :on-close   (fn [ch _]
                 (pws/on-close (->handler ch)))})

(defmethod ig/init-key ::handler [_ cfg]
  (fn [request]
    (when (:websocket? request)
      (->> cfg
           (->handler-fn request)
           ch-map
           (web.async/as-channel request)))))

(defn broadcast!
  "Broadcast an event to all connections"
  [pubsub event-id event]
  (pubsub/publish! pubsub [::broadcast] [event-id event]))

(defn send-user!
  "Broadcast an event to all connections for a specific user"
  [pubsub user-id event-id event]
  (pubsub/publish! pubsub [::user user-id] [event-id event]))

(defn open? [ch]
  (pws/open? ch))

(defn send! [ch msg]
  (pws/send! ch msg))

(defn close! [ch]
  (pws/close! ch))

(defn on-open [handler]
  (pws/on-open handler))

(defn on-close [handler]
  (pws/on-close handler))

(defn on-message [handler msg]
  (pws/on-message handler msg))
