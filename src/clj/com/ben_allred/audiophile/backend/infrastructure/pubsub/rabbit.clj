(ns com.ben-allred.audiophile.backend.infrastructure.pubsub.rabbit
  (:require
    [com.ben-allred.audiophile.common.core.serdes.core :as serdes]
    [com.ben-allred.audiophile.common.core.utils.core :as u]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.common.core.utils.maps :as maps]
    [com.ben-allred.audiophile.common.infrastructure.http.protocols :as phttp]
    [com.ben-allred.audiophile.common.infrastructure.pubsub.protocols :as ppubsub]
    [langohr.basic :as lb]
    [langohr.channel :as lch]
    [langohr.consumers :as lc]
    [langohr.core :as rmq]
    [langohr.queue :as lq]))

(defn ^:private close! [ch]
  (u/silent!
    (some-> ch rmq/close)))

(defn ^:private ->handler [handlers serde]
  (fn [_ metadata ^bytes msg]
    (let [msg (serdes/deserialize serde (String. msg "UTF-8"))]
      (when-not (get-in (log/spy :report "HANDLER" msg) [:event 1 :event/model-id])
        (log/warn "MISSING MODEL_ID" (get-in msg [:event 1 :event/data])))
      (doseq [handler handlers]
        (handler metadata msg)))))

(deftype RabbitMQPublisher [conn ch queue-name serde]
  ppubsub/IPub
  (publish! [_ topic event]
    (let [msg (serdes/serialize serde (maps/->m topic event))]
      (lb/publish ch "" queue-name msg {:content-type (serdes/mime-type serde)})))

  phttp/ICheckHealth
  (display-name [_]
    ::RabbitMQPublisher)
  (healthy? [_]
    (rmq/open? ch))
  (details [_]
    {:queue queue-name
     :serde (serdes/mime-type serde)}))

(defn conn [cfg]
  (rmq/connect cfg))

(defn conn#stop [conn]
  (close! conn))

(defn publisher [{:keys [conn queue-name serde]}]
  (let [ch (lch/open conn)]
    (->RabbitMQPublisher conn ch queue-name serde)))

(deftype RabbitMQSubscriber [ch queue-name serde]
  phttp/ICheckHealth
  (display-name [_]
    ::RabbitMQSubscriber)
  (healthy? [_]
    (rmq/open? ch))
  (details [_]
    {:queue queue-name
     :serde (serdes/mime-type serde)}))

(defn subscriber [{:keys [conn handlers queue-name serde]}]
  (let [ch (lch/open conn)]
    (lq/declare ch queue-name {:exclusive false :auto-delete false})
    (lc/subscribe ch queue-name (->handler handlers serde) {:auto-ack true})
    (->RabbitMQSubscriber ch queue-name serde)))
