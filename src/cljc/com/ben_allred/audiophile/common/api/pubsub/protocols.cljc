(ns com.ben-allred.audiophile.common.api.pubsub.protocols)

(defprotocol IPub
  "Handler for publishing events"
  (publish! [this topic event]
    "Send an event to all subscribers of a topic"))

(defprotocol ISub
  "Handler for subscribing and unsubscribing from events"
  (subscribe! [this key topic listener]
    "Subscribe a `listener` with an associated `key` to the specified `topic`.
     Subscriptions are uniquely identified by (key, topic) and grouped by key.
     Subscribing to the same (key, topic) is undefined behavior and should be
     avoided.")
  (unsubscribe! [this key] [this key topic]
    "Remove all listeners for an associated key from the specified topic or
     from all topics when no topic is specified."))
