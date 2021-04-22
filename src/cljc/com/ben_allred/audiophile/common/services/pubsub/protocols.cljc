(ns com.ben-allred.audiophile.common.services.pubsub.protocols)

(defprotocol IPubSub
  (publish! [this topic event])
  (subscribe! [this key topic listener])
  (unsubscribe! [this key] [this key topic]))
