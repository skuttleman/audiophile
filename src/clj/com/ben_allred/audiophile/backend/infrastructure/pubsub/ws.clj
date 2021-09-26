(ns com.ben-allred.audiophile.backend.infrastructure.pubsub.ws
  (:require
    [clojure.core.async :as async]
    [com.ben-allred.audiophile.backend.domain.interactors.protocols :as pint]
    [com.ben-allred.audiophile.backend.infrastructure.pubsub.core :as ps]
    [com.ben-allred.audiophile.backend.infrastructure.pubsub.protocols :as pps]
    [com.ben-allred.audiophile.common.core.serdes.core :as serdes]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.common.core.utils.uuids :as uuids]
    [com.ben-allred.audiophile.common.infrastructure.pubsub.core :as pubsub]
    [immutant.web.async :as web.async]))

(defmulti ^:private handle-msg (fn [_ _ event]
                                 (log/debug "received event" event)
                                 (when (seqable? event)
                                   (first event))))
(defmethod handle-msg :default
  [_ _ event]
  (log/warn "event not handled" event))

(defmethod handle-msg :conn/ping
  [_ channel _]
  (pps/send! channel [:conn/pong]))

(defmethod handle-msg :conn/pong
  [_ _ _])

(defn ^:private ->sub-handler [channel event-type]
  (fn [_ event]
    (pps/send! channel (into [event-type] event))))

(defn ^:private subscribe* [{:keys [ch-id pubsub]} channel topic event-type]
  (pubsub/subscribe! pubsub ch-id topic (->sub-handler channel event-type)))

(defn ^:private ch-loop [{:keys [heartbeat-int-ms]} channel]
  (async/go-loop []
    (async/<! (async/timeout heartbeat-int-ms))
    (when (pps/open? channel)
      (try
        (pps/send! channel [:conn/ping])
        (catch Throwable _
          (pps/close! channel)))
      (recur))))

(defn ^:private handle-open [{:keys [user-id] :as ctx} channel]
  (subscribe* ctx channel [::ps/broadcast] :event/broadcast)
  (subscribe* ctx channel [::ps/user user-id] :event/user)
  (ch-loop ctx channel))

(defn ^:private handle-close [{:keys [ch-id pubsub user-id]} _]
  (log/info "websocket closed:" ch-id user-id)
  (pubsub/unsubscribe! pubsub ch-id))

(defn ^:private send* [channel serde msg]
  (try
    (let [level (if (#{[:conn/ping] [:conn/pong]} msg) :trace :debug)]
      (log/log level "sending msg to websocket" msg))
    (pps/send! channel
               (cond->> msg
                 serde (serdes/serialize serde)))
    (catch Throwable ex
      (log/error ex "failed to send msg to websocket" msg)
      (throw ex))))

(defn ^:private close* [channel]
  (try
    (pps/close! channel)
    (catch Throwable ex
      (log/warn ex "web socket did not close successfully"))))

(deftype SerdeChannel [channel serializer]
  pps/IChannel
  (open? [_]
    (pps/open? channel))
  (send! [_ msg]
    (send* channel serializer msg))
  (close! [_]
    (close* channel)))

(deftype Channel [ch]
  pps/IChannel
  (open? [_]
    (web.async/open? ch))
  (send! [_ msg]
    (web.async/send! ch msg))
  (close! [_]
    (web.async/close ch)))

(defn ^:private ch-map [->handler]
  {:on-open    (comp pps/on-open ->handler)
   :on-message (fn [ch msg]
                 (pps/on-message (->handler ch) msg))
   :on-close   (fn [ch _]
                 (pps/on-close (->handler ch)))})

(defn ^:private ->handler-fn [params {:keys [->channel ->handler]}]
  (let [handler (promise)]
    (fn [ch]
      (when-not (realized? handler)
        (->> (->Channel ch)
             (->channel params)
             (->handler params)
             (deliver handler)))
      @handler)))

(defn ->channel
  "Constructor for [[SerdeChannel]] used to serialize/deserialize websocket messages."
  [{:keys [serdes]}]
  (fn [params channel]
    (let [serializer (serdes/find-serde! serdes (:accept params))]
      (->SerdeChannel channel serializer))))

(defn ->handler
  "Constructor for an anonymous [[pws/IHandler]] that links websocket connections with [[ppubsub/IPubsub]]."
  [{:keys [heartbeat-int-ms pubsub serdes]}]
  (fn [params channel]
    (let [deserializer (serdes/find-serde serdes (:content-type params))
          ctx {:ch-id            (uuids/random)
               :heartbeat-int-ms heartbeat-int-ms
               :user-id          (:user/id params)
               :pubsub           pubsub
               :state            (ref nil)}]
      (reify
        pps/IHandler
        (on-open [_]
          (handle-open ctx channel))
        (on-message [_ msg]
          (handle-msg ctx channel (cond->> msg
                                    deserializer (serdes/deserialize deserializer))))
        (on-close [_]
          (handle-close ctx channel))))))

(defn handler
  "Wraps ws handler to integrate with immutant HTTP server for websockets."
  [cfg]
  (fn [request]
    (->> cfg
         (->handler-fn request)
         ch-map
         (web.async/as-channel request))))

(deftype WebSocketMessageHandler [pubsub]
  pint/IMessageHandler
  (handle! [_ {event-id :event/id :event/keys [ctx] :as event}]
    (try
      (log/info "publishing event to ws" event-id)
      (ps/send-user! pubsub (:user/id ctx) event-id (dissoc event :event/ctx) ctx)
      (catch Throwable ex
        (log/error ex "failed: publishing event to ws" event)))))

(defn event->ws-handler [{:keys [pubsub]}]
  (->WebSocketMessageHandler pubsub))
