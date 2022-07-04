(ns audiophile.backend.api.pubsub.protocols)

(defprotocol IChannel
  "A WebSocket channel"
  (open? [this] "Is the connection open?")
  (send! [this msg] "Send to a topic")
  (close! [this] "When open, closes the connection"))
