(ns com.ben-allred.audiophile.api.services.pubsub.ws
  (:require
    [clojure.core.async :as async]
    [com.ben-allred.audiophile.common.services.pubsub.core :as pubsub]
    [com.ben-allred.audiophile.common.services.serdes.core :as serdes]
    [com.ben-allred.audiophile.common.utils.core :as u]
    [com.ben-allred.audiophile.common.utils.logger :as log]
    [com.ben-allred.audiophile.common.utils.uuids :as uuids]
    [immutant.web.async :as web.async]
    [integrant.core :as ig]))

(defprotocol IChannel
  "A WebSocket channel"
  (open? [this] "is the connection open?")
  (send! [this msg] "send to a topic")
  (close! [this] "when open, closes the connection"))

(defprotocol IHandler
  "Handles WebSocket events"
  (on-open [this] "Callback indicating a connection has been established")
  (on-message [this msg] "Callback for handling a received message")
  (on-close [this] "Callback indicating when a connection has been established"))

(defmulti ^:private handle-msg (fn [_ _ event]
                                 (log/debug "received event" event)
                                 (when (seqable? event)
                                   (first event))))
(defmethod handle-msg :default
  [_ _ event]
  (log/warn "event not handled" event))

(defmethod handle-msg :conn/ping
  [_ channel _]
  (send! channel [:conn/pong]))

(defmethod handle-msg :conn/pong
  [_ _ _])

(defn ^:private handle-open [{:keys [ch-id heartbeat-int-ms pubsub user-id]} channel]
  (pubsub/subscribe! pubsub
                     ch-id
                     [::broadcast]
                     (fn [_ [event-id event]]
                       (send! channel [:event/broadcast event-id event])))
  (pubsub/subscribe! pubsub
                     ch-id
                     [::user user-id]
                     (fn [_ [event-id event]]
                       (send! channel [:event/user event-id event {:user/id user-id}])))
  (async/go-loop []
    (async/<! (async/timeout heartbeat-int-ms))
    (when (open? channel)
      (try
        (send! channel [:conn/ping])
        (catch Throwable _
          (u/silent! (close! channel))))
      (recur))))

(defn ^:private handle-close [{:keys [ch-id pubsub user-id]} _]
  (log/info "websocket closed:" user-id)
  (pubsub/unsubscribe! pubsub ch-id))

(defmethod ig/init-key ::->handler [_ {:keys [heartbeat-int-ms pubsub serdes]}]
  (fn [request channel]
    (let [user-id (get-in request [:auth/user :data :user :user/id])
          params (get-in request [:nav/route :query-params])
          deserializer (serdes/find-serde serdes
                                          (or (:content-type params)
                                              (:accept params)
                                              ""))
          ctx {:ch-id            (uuids/random)
               :heartbeat-int-ms heartbeat-int-ms
               :user-id          user-id
               :pubsub           pubsub
               :state            (ref nil)}]
      (reify
        IHandler
        (on-open [_]
          (handle-open ctx channel))
        (on-message [_ msg]
          (handle-msg ctx channel (serdes/deserialize deserializer msg)))
        (on-close [_]
          (handle-close ctx channel))))))

(defmethod ig/init-key ::->channel [_ {:keys [serdes]}]
  (fn [request channel]
    (let [params (get-in request [:nav/route :query-params])
          serializer (serdes/find-serde serdes
                                        (or (:accept params)
                                            (:content-type params)
                                            ""))]
      (reify
        IChannel
        (open? [_]
          (open? channel))
        (send! [_ msg]
          (try
            (send! channel (serdes/serialize serializer msg))
            (catch Throwable ex
              (log/warn ex "failed to send msg to websocket" msg))))
        (close! [_]
          (try
            (close! channel)
            (catch Throwable ex
              (log/warn ex "web socket did not close successfully"))))))))

(deftype Channel [ch]
  IChannel
  (open? [_]
    (web.async/open? ch))
  (send! [_ msg]
    (web.async/send! ch msg))
  (close! [_]
    (web.async/close ch)))

(defmethod ig/init-key ::handler [_ {:keys [->channel ->handler]}]
  (fn [request]
    (when (:websocket? request)
      (let [handler (promise)
            deref* (fn [ch]
                     (when-not (realized? handler)
                       (->> (->Channel ch)
                            (->channel request)
                            (->handler request)
                            (deliver handler)))
                     @handler)]
        (web.async/as-channel request
                              {:on-open    (comp on-open deref*)
                               :on-message (fn [ch msg]
                                             (on-message (deref* ch) msg))
                               :on-close   (fn [ch _]
                                             (on-close (deref* ch)))})))))

(defn broadcast! [pubsub event-id event]
  (pubsub/publish! pubsub [::broadcast] [event-id event]))

(defn send-user! [pubsub user-id event-id event]
  (pubsub/publish! pubsub [::user user-id] [event-id event]))
