(ns com.ben-allred.audiophile.backend.infrastructure.pubsub.protocols)

(defprotocol IChannel
  "A WebSocket channel"
  (open? [this] "Is the connection open?")
  (send! [this msg] "Send to a topic")
  (close! [this] "When open, closes the connection"))

(defprotocol IHandler
  "Handles WebSocket events"
  (on-open [this] "Callback indicating a connection has been established")
  (on-message [this msg] "Callback for handling a received message")
  (on-close [this] "Callback indicating when a connection has been closed"))

(defprotocol IMQConnection
  "Abstraction for an MQ Connection"
  (chan [this opts] "Creates an [[IMQChannel]] from an open connection"))

(defprotocol IMQChannel
  "Abstraction for an MQ Channel"
  (subscribe! [this handler opts] "Subscribes to a queue with a callback handler"))
