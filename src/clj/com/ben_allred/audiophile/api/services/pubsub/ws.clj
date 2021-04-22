(ns com.ben-allred.audiophile.api.services.pubsub.ws
  (:require
    [clojure.core.async :as async]
    [com.ben-allred.audiophile.common.services.pubsub.core :as pubsub]
    [com.ben-allred.audiophile.common.services.serdes.core :as serdes]
    [com.ben-allred.audiophile.common.utils.http :as http]
    [com.ben-allred.audiophile.common.utils.logger :as log]
    [com.ben-allred.audiophile.common.utils.uuids :as uuids]
    [immutant.web.async :as web.async]
    [integrant.core :as ig]))

(defmulti ^:private take-action! (fn [event _]
                                   (when (seqable? event)
                                     (first event))))
(defmethod take-action! :default
  [event _]
  (log/warn "unknown event type" event))

(defmethod take-action! :conn/ping
  [_ {:keys [ch send-fn]}]
  (send-fn ch [:conn/pong]))

(defmethod take-action! :conn/pong
  [_ _])

(defn ^:private handle-on-open [{:keys [ch-id heartbeat-int pubsub send-fn user-id]}]
  (fn [ch]
    (pubsub/subscribe! pubsub
                       ch-id
                       [::broadcast]
                       (fn [_ [event-id event]]
                         (send-fn ch [:event/broadcast event-id event])))
    (pubsub/subscribe! pubsub
                       ch-id
                       [::user user-id]
                       (fn [_ [event-id event]]
                         (send-fn ch [:event/user event-id event {:user/id user-id}])))
    (async/go-loop []
      (async/<! (async/timeout (* 1000 heartbeat-int)))
      (when (web.async/open? ch)
        (try
          (send-fn ch [:conn/ping])
          (catch Throwable _
            (try
              (web.async/close ch)
              (catch Throwable _))))
        (recur)))))

(defn ^:private handle-on-close [{:keys [ch-id pubsub user-id]}]
  (fn [_ reason]
    (log/debug "websocket closed:" ch-id reason)
    (log/info "websocket closed:" user-id)
    (pubsub/unsubscribe! pubsub ch-id)))

(defmethod ig/init-key ::handler [_ {:keys [heartbeat-int pubsub serdes]}]
  (fn [request]
    (if-not (:auth/user request)
      {:status ::http/unauthorized :body {:errors [{:message "you must be logged in"}]}}
      (when (:websocket? request)
        (let [serializer (serdes/find-serde serdes (or (get-in request [:nav/route :query-params :accept])
                                                       (get-in request [:nav/route :query-params :content-type])
                                                       ""))
              deserializer (serdes/find-serde serdes (or (get-in request [:nav/route :query-params :content-type])
                                                         (get-in request [:nav/route :query-params :accept])
                                                         ""))
              ch-id (uuids/random)
              user-id (get-in request [:auth/user :data :user :user/id])
              ctx {:ch-id         ch-id
                   :heartbeat-int heartbeat-int
                   :user-id       user-id
                   :pubsub        pubsub
                   :send-fn       (fn [ch msg]
                                    (log/debug "sending message" ch-id msg)
                                    (web.async/send! ch (serdes/serialize serializer msg)))
                   :state         (ref nil)}]
          (log/info "connecting websocket:" user-id)
          (-> request
              (web.async/as-channel {:on-open    (handle-on-open ctx)
                                     :on-message (fn [ch event]
                                                   (let [msg (serdes/deserialize deserializer event)]
                                                     (log/debug "receiving message" ch-id msg)
                                                     (take-action! msg (assoc ctx :ch ch))))
                                     :on-close   (handle-on-close ctx)})
              (assoc :serialized? true)))))))

(defn broadcast! [pubsub event-id event]
  (pubsub/publish! pubsub [::broadcast] [event-id event]))

(defn send-user! [pubsub user-id event-id event]
  (pubsub/publish! pubsub [::user user-id] [event-id event]))
