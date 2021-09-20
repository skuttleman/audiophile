(ns com.ben-allred.audiophile.backend.infrastructure.pubsub.rabbit
  (:require
    [com.ben-allred.audiophile.common.infrastructure.http.protocols :as phttp]
    [com.ben-allred.audiophile.common.core.serdes.core :as serdes]
    [com.ben-allred.audiophile.common.core.utils.core :as u]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.common.core.utils.maps :as maps]
    [com.ben-allred.audiophile.common.infrastructure.pubsub.protocols :as ppubsub]
    [langohr.basic :as lb]
    [langohr.channel :as lch]
    [langohr.consumers :as lc]
    [langohr.core :as rmq]
    [langohr.queue :as lq])
  (:import
    (java.io Closeable)))

(defn ^:private close! [ch]
  (u/silent!
    (some-> ch rmq/close)))

(defn ^:private ->handler [handlers serde]
  (fn [_ metadata ^bytes msg]
    (let [msg (serdes/deserialize serde (String. msg "UTF-8"))]
      (doseq [handler handlers]
        (handler metadata msg)))))

(deftype RabbitMQPubSub [conn ch pubsub queue-name serde]
  ppubsub/IPubSub
  (publish! [_ topic event]
    (let [msg (serdes/serialize serde (maps/->m topic event))]
      (lb/publish ch "" queue-name msg {:content-type (serdes/mime-type serde)})))
  (subscribe! [_ key topic listener]
    (ppubsub/subscribe! pubsub key topic listener))
  (unsubscribe! [_ key]
    (ppubsub/unsubscribe! pubsub key))
  (unsubscribe! [_ key topic]
    (ppubsub/unsubscribe! pubsub key topic))

  phttp/ICheckHealth
  (display-name [_]
    ::RabbitMQPubSub)
  (healthy? [_]
    (rmq/open? ch))
  (details [_]
    nil)

  Closeable
  (close [_]
    (close! conn)))

(defn rabbitmq [{:keys [cfg handlers pubsub queue-name serde]}]
  (let [conn (rmq/connect cfg)
        ch (lch/open conn)]
    (lq/declare ch queue-name {:exclusive false :auto-delete false})
    (lc/subscribe ch queue-name (->handler handlers serde) {:auto-ack true})
    (->RabbitMQPubSub conn ch pubsub queue-name serde)))

(defn rabbitmq#stop [rabbitmq]
  (.close ^Closeable rabbitmq))
