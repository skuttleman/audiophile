(ns com.ben-allred.audiophile.backend.infrastructure.pubsub.rabbit
  (:require
    [com.ben-allred.audiophile.backend.domain.interactors.core :as int]
    [com.ben-allred.audiophile.backend.api.pubsub.protocols :as pps]
    [com.ben-allred.audiophile.common.core.serdes.core :as serdes]
    [com.ben-allred.audiophile.common.core.utils.core :as u]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.common.infrastructure.http.protocols :as phttp]
    [langohr.basic :as lb]
    [langohr.channel :as lch]
    [langohr.consumers :as lc]
    [langohr.core :as rmq]
    [langohr.exchange :as le]
    [langohr.queue :as lq])
  (:import
    (java.io Closeable)))

(deftype RabbitMQFanoutChannel [ch exchange queue-name serde ch-opts]
  pps/IChannel
  (open? [_]
    (rmq/open? ch))
  (send! [_ msg]
    (let [msg (serdes/serialize serde msg)]
      (log/info "publishing to" exchange)
      (lb/publish ch exchange "" msg {:content-type (serdes/mime-type serde)})))
  (close! [_]
    (u/silent!
      (some-> ch rmq/close)))

  pps/IMQChannel
  (subscribe! [_ handler opts]
    (letfn [(handler* [_ch _metadata ^bytes msg]
              (let [msg (serdes/deserialize serde (String. msg "UTF-8"))]
                (log/with-ctx (or (:command/ctx msg) (:event/ctx msg))
                  (log/info "consuming from" exchange)
                  (int/handle! handler msg))))]
      (let [queue-name (if-let [handler (:internal/handler opts)]
                         (let [queue-name (str queue-name ":" handler)]
                           (lq/declare ch queue-name ch-opts)
                           (lq/bind ch queue-name exchange)
                           queue-name)
                         queue-name)]
        (log/info "subscribing" queue-name "to" exchange)
        (lc/subscribe ch queue-name handler* (dissoc opts :internal/handler))))))

(defmulti ->channel :type)

(defmethod ->channel :fanout
  [cfg]
  (let [ch (lch/open (::conn cfg))
        exchange (:name cfg)
        queue-name (str exchange "." (:consumer cfg))
        ch-opts (:opts cfg)]
    (le/declare ch exchange "fanout" ch-opts)
    (lq/declare ch queue-name ch-opts)
    (lq/bind ch queue-name exchange)
    (->RabbitMQFanoutChannel ch exchange queue-name (:serde cfg) ch-opts)))

(deftype RabbitMQConnection [conn]
  pps/IMQConnection
  (chan [_ cfg]
    (->channel (assoc cfg ::conn conn)))

  phttp/ICheckHealth
  (display-name [_]
    ::RabbitMQConnection)
  (healthy? [_]
    (rmq/open? conn))
  (details [_]
    nil)

  Closeable
  (close [_]
    (u/silent!
      (some-> conn rmq/close))))

(defn conn [{:keys [cfg]}]
  (->RabbitMQConnection (rmq/connect cfg)))

(defn conn#stop [conn]
  (.close ^Closeable conn))

(defn channel [{:keys [conn queue-cfg]}]
  (pps/chan conn queue-cfg))

(defn subscriber [{:keys [ch handler opts]}]
  (pps/subscribe! ch handler opts))

(defn exchange [{:keys [name namespace]}]
  (str namespace "." name))
