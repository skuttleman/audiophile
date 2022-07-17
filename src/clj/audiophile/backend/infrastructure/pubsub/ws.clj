(ns audiophile.backend.infrastructure.pubsub.ws
  (:require
    [audiophile.backend.api.pubsub.core :as ps]
    [audiophile.backend.api.pubsub.protocols :as pps]
    [audiophile.backend.api.validations.selectors :as selectors]
    [audiophile.common.api.pubsub.core :as pubsub]
    [audiophile.common.core.serdes.core :as serdes]
    [audiophile.common.core.serdes.impl :as serde]
    [audiophile.common.core.utils.core :as u]
    [audiophile.common.core.utils.logger :as log]
    [audiophile.common.core.utils.uuids :as uuids]
    [clojure.core.async :as async]
    [immutant.web.async :as web.async]
    [spigot.context :as sp.ctx])
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

(defmethod on-message! :sub/start!
  [ctx ch [_ topic]]
  (u/silent!
    (subscribe* ctx ch topic :event/subscription)))

(defmethod on-message! :sub/stop!
  [{::keys [pubsub ch-id]} _ [_ topic]]
  (pubsub/unsubscribe! pubsub ch-id topic))

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

(defn ^:private extract-result [data workflow-id]
  (-> data
      :workflows/->result
      (sp.ctx/resolve-params (:ctx data))
      (assoc :workflow/id workflow-id
             :workflow/template (:workflows/template data))))

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

(defn event-handler [{:keys [pubsub]}]
  (fn [{{event-id :event/id :event/keys [ctx type] :as event} :value}]
    (when-let [event (case type
                       :workflow/completed (update event
                                                   :event/data
                                                   extract-result
                                                   (:workflow/id ctx))
                       :workflow/failed event
                       nil)]
      (let [event (dissoc event :event/ctx)]
        (log/with-ctx :CP
          (log/info "publishing event to ws" event-id)
          (ps/send-user! pubsub (:user/id ctx) event-id event ctx))))))
