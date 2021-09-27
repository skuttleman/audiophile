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
    [immutant.web.async :as web.async])
  (:import
    (org.projectodd.wunderboss.web.async Channel)))

(defn ^:private ch-loop [{::keys [heartbeat-int-ms]} channel]
  (async/go-loop []
    (async/<! (async/timeout heartbeat-int-ms))
    (when (pps/open? channel)
      (try
        (pps/send! channel [:conn/ping])
        (catch Throwable _
          (pps/close! channel)))
      (recur))))

(defn ^:private ->sub-handler [ch event-type]
  (fn [_ event]
    (pps/send! ch (into [event-type] event))))

(defn ^:private subscribe* [{::keys [ch-id pubsub]} ch topic event-type]
  (pubsub/subscribe! pubsub ch-id topic (->sub-handler ch event-type)))

(defmulti on-message! (fn [_ _ msg]
                       (log/debug "received event" msg)
                       (when (seqable? msg)
                         (first msg))))

(defmethod on-message! :default
  [_ _ msg]
  (log/warn "event not handled" msg))

(defmethod on-message! :conn/ping
  [_ ch _]
  (pps/send! ch [:conn/pong]))

(defmethod on-message! :conn/pong
  [_ _ _])

(defn on-open! [{::keys [user-id] :as ctx} ch]
  (subscribe* ctx ch [::ps/broadcast] :event/broadcast)
  (subscribe* ctx ch [::ps/user user-id] :event/user)
  (ch-loop ctx ch))

(defn on-close! [{::keys [ch-id pubsub user-id]} _]
  (log/info "websocket closed:" ch-id user-id)
  (pubsub/unsubscribe! pubsub ch-id))

(defn build-ctx [request pubsub heartbeat-int-ms]
  (assoc request
         ::ch-id (uuids/random)
         ::user-id (:user/id request)
         ::heartbeat-int-ms heartbeat-int-ms
         ::pubsub pubsub))

(deftype WebSocketChannel [^Channel ch serde]
  pps/IChannel
  (open? [_]
    (web.async/open? ch))
  (send! [_ msg]
    (try
      (let [level (if (#{[:conn/ping] [:conn/pong]} msg) :trace :debug)]
        (log/log level "sending msg to websocket" msg)
        (web.async/send! ch (cond->> msg
                                     serde (serdes/serialize serde))))
      (catch Throwable ex
        (log/error ex "failed to send msg to websocket" msg)
        (throw ex))))
  (close! [_]
    (try
      (web.async/close ch)
      (catch Throwable ex
        (log/warn ex "web socket did not close successfully")))))

(defn handler [{:keys [heartbeat-int-ms pubsub serdes]}]
  (fn [request]
    (let [serde (serdes/find-serde! serdes (log/spy :info (:accept request)))
          ctx (build-ctx request pubsub heartbeat-int-ms)]
      (web.async/as-channel request
                            {:on-open    (fn [ch]
                                           (let [ch (->WebSocketChannel ch serde)]
                                             (on-open! ctx ch)))
                             :on-message (fn [ch msg]
                                           (let [ch (->WebSocketChannel ch serde)]
                                             (->> msg
                                                  (serdes/deserialize serde)
                                                  (on-message! ctx ch))))
                             :on-close   (fn [ch _]
                                           (on-close! ctx ch))}))))

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
