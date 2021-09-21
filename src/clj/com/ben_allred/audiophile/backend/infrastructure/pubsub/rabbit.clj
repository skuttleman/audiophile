(ns com.ben-allred.audiophile.backend.infrastructure.pubsub.rabbit
  (:require
    [com.ben-allred.audiophile.backend.infrastructure.pubsub.protocols :as pps]
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
    [langohr.queue :as lq])
  (:import
    (java.io Closeable)))

(defn ^:private ->handler [handlers]
  (fn [msg]
    (doseq [handler handlers]
      (handler msg))))

(deftype RabbitMQPublisher [ch]
  ppubsub/IPub
  (publish! [_ topic event]
    (pps/send! ch (maps/->m topic event))))

(deftype RabbitMQChannel [ch queue-name serde]
  pps/IChannel
  (open? [_]
    (rmq/open? ch))
  (send! [_ msg]
    (let [msg (serdes/serialize serde msg)]
      (lb/publish ch "" queue-name msg {:content-type (serdes/mime-type serde)})))
  (close! [_]
    (u/silent!
      (some-> ch rmq/close)))

  pps/IMQChannel
  (subscribe! [_ handler opts]
    (letfn [(handler* [_ch _metadata ^bytes msg]
              (let [msg (serdes/deserialize serde (String. msg "UTF-8"))]
                (handler msg)))]
      (lc/subscribe ch queue-name handler* opts))))

(deftype RabbitMQConnection [conn queue-name serde]
  pps/IMQConnection
  (chan [_ opts]
    (let [ch (lch/open conn)]
      (lq/declare ch queue-name opts)
      (->RabbitMQChannel ch queue-name serde)))

  phttp/ICheckHealth
  (display-name [_]
    ::RabbitMQConnection)
  (healthy? [_]
    (rmq/open? conn))
  (details [_]
    {:queue queue-name
     :serde (serdes/mime-type serde)})

  Closeable
  (close [_]
    (u/silent!
      (some-> conn rmq/close))))

(defn conn [{:keys [cfg queue-name serde]}]
  (->RabbitMQConnection (rmq/connect cfg) queue-name serde))

(defn conn#stop [conn]
  (.close ^Closeable conn))

(defn publisher [{:keys [conn]}]
  (let [ch (pps/chan conn {:exclusive false :auto-delete false})]
    (->RabbitMQPublisher ch)))

(defn subscriber [{:keys [conn handlers]}]
  (let [ch (pps/chan conn {:exclusive false :auto-delete false})]
    (pps/subscribe! ch (->handler handlers) {:auto-ack true})))
