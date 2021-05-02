(ns com.ben-allred.audiophile.api.services.pubsub.protocols)

(defprotocol IChannel
  "A WebSocket channel"
  (open? [this] "is the connection open?")
  (send! [this msg] "send to a topic")
  (close! [this] "when open, closes the connection"))

(defprotocol IHandler
  "Handles WebSocket events"
  (on-open [this] "Callback indicating a connection has been established")
  (on-message [this msg] "Callback for handling a received message")
  (on-close [this] "Callback indicating when a connection has been established"))
