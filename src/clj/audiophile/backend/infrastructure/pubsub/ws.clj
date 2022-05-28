(ns audiophile.backend.infrastructure.pubsub.ws
  (:require
    [clojure.core.async :as async]
    [audiophile.backend.api.pubsub.core :as ps]
    [audiophile.backend.api.pubsub.protocols :as pps]
    [audiophile.backend.api.validations.selectors :as selectors]
    [audiophile.backend.core.serdes.jwt :as jwt]
    [audiophile.backend.domain.interactors.protocols :as pint]
    [audiophile.common.api.pubsub.core :as pubsub]
    [audiophile.common.core.serdes.core :as serdes]
    [audiophile.common.core.serdes.impl :as serde]
    [audiophile.common.core.utils.logger :as log]
    [audiophile.common.core.utils.uuids :as uuids]
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
  (log/with-ctx :WS
    (log/warn "event not handled" msg)))

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
  (log/with-ctx :WS
    (log/info ch-id "closed for" user-id))
  (pubsub/unsubscribe! pubsub ch-id))

(defn build-ctx [request pubsub heartbeat-int-ms]
  (assoc request
         ::ch-id (uuids/random)
         ::user-id (:user/id request)
         ::heartbeat-int-ms heartbeat-int-ms
         ::pubsub pubsub))

(defn ^:private web-socket-channel#send! [this ^Channel ch serde msg]
  (log/with-ctx [this :WS]
    (try
      (log/trace "sending msg to websocket" msg)
      (web.async/send! ch (cond->> msg
                            serde (serdes/serialize serde)))
      (catch Throwable ex
        (log/error ex "failed to send msg to websocket" msg)
        (throw ex)))))

(defn ^:private web-socket-channel#close! [this ^Channel ch]
  (log/with-ctx [this :WS]
    (try
      (web.async/close ch)
      (catch Throwable ex
        (log/warn ex "web socket did not close successfully")))))

(deftype WebSocketChannel [^Channel ch serde]
  pps/IChannel
  (open? [_]
    (web.async/open? ch))
  (send! [this msg]
    (web-socket-channel#send! this ch serde msg))
  (close! [this]
    (web-socket-channel#close! this ch)))

(defn handler [{:keys [heartbeat-int-ms pubsub]}]
  (fn [request]
    (let [serde (serdes/find-serde! serde/serdes (:accept request))
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

(defmethod selectors/select [:get :routes.ws/connection]
  [_ request]
  (-> request
      (assoc :user/id (get-in request [:auth/user :user/id])
             :token/aud (get-in request [:auth/user :jwt/aud]))
      (merge (:headers request) (get-in request [:nav/route :params]))))

(defmulti handle-event* (fn [_ _ event]
                          (:event/type event)))

(defmethod handle-event* :user/created
  [pubsub jwt-serde {event-id :event/id :event/keys [ctx] :as event}]
  (let [token (jwt/login-token jwt-serde (:event/data event))]
    (ps/send-user! pubsub
                   (:signup/id ctx)
                   event-id
                   (-> event
                       (dissoc :event/ctx)
                       (assoc-in [:event/data :login/token] token))
                   ctx)))

(defmethod handle-event* :default
  [pubsub _ {event-id :event/id :event/keys [ctx] :as event}]
  (ps/send-user! pubsub
                 (or (:signup/id ctx) (:user/id ctx))
                 event-id
                 (dissoc event :event/ctx)
                 ctx))

(defn ^:private web-socket-message-handler#handle!
  [this pubsub jwt-serde {event-id :event/id :as event}]
  (log/with-ctx [this :CP]
    (log/info "publishing event to ws" event-id)
    (handle-event* pubsub jwt-serde event)))

(deftype WebSocketMessageHandler [pubsub jwt-serde]
  pint/IMessageHandler
  (handle? [_ _]
    true)
  (handle! [this msg]
    (web-socket-message-handler#handle! this pubsub jwt-serde msg)))

(defn event->ws-handler [{:keys [jwt-serde pubsub]}]
  (->WebSocketMessageHandler pubsub jwt-serde))
