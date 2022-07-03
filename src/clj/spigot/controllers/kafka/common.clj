(ns spigot.controllers.kafka.common
  (:require
    [kinsky.client :as client*])
  (:import
    (java.util Properties)
    (org.apache.kafka.common.serialization Serde Serializer Deserializer)))

(deftype SpigotSerde [^Serializer serializer ^Deserializer deserializer]
  Serde
  (serializer [_]
    serializer)
  (deserializer [_]
    deserializer))

(defn default-serde []
  (->SpigotSerde (client*/edn-serializer) (client*/edn-deserializer)))

(defn create-serde [^Serializer serializer ^Deserializer deserializer]
  (->SpigotSerde serializer deserializer))

(defn ->props ^Properties [m]
  (let [props (Properties.)]
    (doseq [e m
            :let [k (key e)
                  v (val e)]]
      (.put props (name k) (cond-> v (keyword? v) name)))
    props))

(defn ->topic-cfg [topic]
  {:name topic
   :key-serde (default-serde)
   :val-serde (default-serde)})
