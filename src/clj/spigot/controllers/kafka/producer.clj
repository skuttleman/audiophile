(ns spigot.controllers.kafka.producer
  (:require
    [spigot.controllers.kafka.protocols :as sp.kproto]
    [kinsky.client :as client*])
  (:import
    (org.apache.kafka.common.serialization Serde)
    (java.lang AutoCloseable)
    (java.io Closeable)))

(deftype SpigotProducer [kinsky-producer topic-cfg]
  sp.kproto/ISpigotProducer
  (send! [_ k v opts]
    (client*/send! kinsky-producer
                   (assoc opts
                          :topic (:name topic-cfg)
                          :key k
                          :value v)))

  AutoCloseable
  (close [_]
    (.close ^Closeable @kinsky-producer)))

(defn client [props {:keys [^Serde key-serde ^Serde val-serde] :as topic-cfg}]
  (->SpigotProducer (client*/producer props
                                      (.serializer key-serde)
                                      (.serializer val-serde))
                    topic-cfg))

(defn send!
  ([producer k v]
   (send! producer k v nil))
  ([producer k v opts]
   (sp.kproto/send! producer k v opts)))
