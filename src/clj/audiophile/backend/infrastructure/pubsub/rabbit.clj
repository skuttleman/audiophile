(ns audiophile.backend.infrastructure.pubsub.rabbit
  (:require
    [audiophile.backend.api.pubsub.core :as ps]
    [audiophile.backend.api.pubsub.protocols :as pps]
    [audiophile.backend.domain.interactors.core :as int]
    [audiophile.common.core.serdes.core :as serdes]
    [audiophile.common.core.serdes.impl :as serde]
    [audiophile.common.core.utils.core :as u]
    [audiophile.common.core.utils.logger :as log]
    [audiophile.common.core.utils.maps :as maps]
    [audiophile.common.core.utils.uuids :as uuids]
    [audiophile.common.infrastructure.http.protocols :as phttp]
    [langohr.basic :as lb]
    [langohr.channel :as lch]
    [langohr.consumers :as lc]
    [langohr.core :as rmq]
    [langohr.exchange :as le]
    [langohr.queue :as lq])
  (:import
    (java.io Closeable)
    (java.nio.charset StandardCharsets)))

(defn ^:private ->handler [handler exchange serde]
  (fn [_ch _metadata ^bytes msg]
    (try
      (let [msg (serdes/deserialize serde (String. msg StandardCharsets/UTF_8))
            ctx (or (:command/ctx msg) (:event/ctx msg))
            msg* (select-keys msg #{:event/type :command/type})]
        (when (int/handle? handler msg)
          (log/with-ctx (assoc ctx :logger/id :MQ)
            (log/info "consuming from" exchange msg*)
            (int/handle! handler msg))))
      (catch Throwable ex
        (log/error ex "an error occurred while handling msg")))))

(deftype RabbitMQFanoutChannel [ch exchange queue-name serde ch-opts]
  pps/IChannel
  (open? [_]
    (rmq/open? ch))
  (send! [this msg]
    (let [msg' (serdes/serialize serde msg)]
      (log/with-ctx [this :MQ]
        (log/info "publishing to" exchange (select-keys msg #{:event/type :command/type}))
        (lb/publish ch exchange "" msg' {:content-type (serdes/mime-type serde)}))))
  (close! [_]
    (u/silent!
      (some-> ch rmq/close)))

  pps/IMQChannel
  (subscribe! [_ handler opts]
    (let [queue-name (if-let [handler (:internal/handler opts)]
                       (let [queue-name (str queue-name ":" handler)]
                         (lq/declare ch queue-name ch-opts)
                         (lq/bind ch queue-name exchange)
                         queue-name)
                       queue-name)
          handler* (->handler handler exchange serde)]
      (log/with-ctx :MQ
        (log/info "subscribing" queue-name "to" exchange)
        (lc/subscribe ch queue-name handler* (dissoc opts :internal/handler))))))

(defmulti ->channel :type)

(defmethod ->channel :fanout
  [cfg]
  (let [ch (lch/open (::conn cfg))
        exchange (:name cfg)
        queue-name (str exchange
                        "."
                        (:consumer cfg)
                        (when (:global? cfg)
                          (str "." (uuids/random))))
        ch-opts (-> cfg (:opts)
                    (assoc :auto-delete (boolean (:global? cfg)))
                    (maps/assoc-defaults :max-retries 2))]
    (le/declare ch exchange "fanout" (:opts cfg))
    (lq/declare ch queue-name ch-opts)
    (lq/bind ch queue-name exchange)
    (->RabbitMQFanoutChannel ch exchange queue-name serde/edn ch-opts)))

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
  (ps/chan conn queue-cfg))

(defn subscriber [{:keys [ch handler opts]}]
  (ps/subscribe! ch handler opts))

(defn exchange [{:keys [name namespace]}]
  (str "audiophile." namespace "." name))