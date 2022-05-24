(ns audiophile.backend.api.pubsub.protocols)

(defprotocol IChannel
  "A WebSocket channel"
  (open? [this] "Is the connection open?")
  (send! [this msg] "Send to a topic")
  (close! [this] "When open, closes the connection"))

(defprotocol IMQConnection
  "Abstraction for an MQ Connection"
  (chan [this cfg] "Creates an [[IMQChannel]] from an open connection"))

(defprotocol IMQChannel
  "Abstraction for an MQ Channel"
  (subscribe! [this handler opts] "Subscribes to a queue with a callback handler"))
