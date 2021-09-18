(ns com.ben-allred.audiophile.backend.infrastructure.pubsub.rabbit
  (:require
    [com.ben-allred.audiophile.common.core.serdes.core :as serdes]
    [com.ben-allred.audiophile.common.core.utils.core :as u]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.common.infrastructure.pubsub.protocols :as ppubsub]
    [langohr.basic :as lb]
    [langohr.channel :as lch]
    [langohr.consumers :as lc]
    [langohr.core :as rmq]
    [langohr.queue :as lq])
  (:import
    (java.io Closeable)))

(defn ^:private message-handler [serde topic listener]
  (fn [_ch metadata ^bytes msg]
    (log/debug "[PubSub] received msg for topic" topic metadata)
    (listener topic (serdes/deserialize serde (String. msg "UTF-8")))))

(defn ^:private close! [ch]
  (u/silent!
    (some-> ch rmq/close)))

(defn ^:private publish* [conn serde topic event]
  (let [queue-name (pr-str topic)
        msg (serdes/serialize serde event)]
    (with-open [ch (lch/open conn)]
      (lq/declare ch queue-name {:exclusive false :auto-delete false})
      (lb/publish ch "" queue-name msg {:content-type (serdes/mime-type serde)}))))

(defn ^:private subscribe* [state conn key topic listener]
  (let [queue-name (pr-str topic)
        ch (lch/open conn)]
    (swap! state (fn [state*]
                   (-> state*
                       (assoc-in [::keys key topic] ch)
                       (update-in [::topics topic key] (fn [ch*]
                                                         (close! ch*)
                                                         ch)))))
    (lq/declare ch queue-name {:exclusive false :auto-delete false})
    (lc/subscribe ch queue-name listener {:auto-ack true})))

(defn ^:private unsubscribe-all [state key]
  (let [topics (keys (get-in @state [::keys key]))]
    (doseq [topic topics
            :let [ch (get-in @state [::topics topic key])]]
      (close! ch))
    (swap! state (fn [state*]
                   (reduce (fn [state* topic]
                             (update-in state* [::topics topic] dissoc key))
                           (update state* ::keys dissoc key)
                           topics)))))

(defn ^:private unsubscribe* [state key topic]
  (let [ch (get-in @state [::topics topic key])]
    (close! ch)
    (swap! state (fn [state*]
                   (-> state*
                       (update ::keys dissoc key)
                       (update-in [::topics topic] dissoc key))))))

(deftype RabbitPubSub [state conn serde]
  ppubsub/IPubSub
  (publish! [_ topic event]
    (publish* conn serde topic event))
  (subscribe! [_ key topic listener]
    (subscribe* state conn key topic (message-handler serde topic listener)))
  (unsubscribe! [_ key]
    (unsubscribe-all state key))
  (unsubscribe! [_ key topic]
    (unsubscribe* state key topic))

  Closeable
  (close [_]
    (close! conn)))

(defn rabbitmq [{:keys [conn serde]}]
  (->RabbitPubSub (atom nil) (rmq/connect conn) serde))

(defn rabbitmq#stop [rabbitmq]
  (.close ^Closeable rabbitmq))
