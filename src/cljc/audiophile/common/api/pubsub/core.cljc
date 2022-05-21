(ns audiophile.common.api.pubsub.core
  (:require
    [audiophile.common.api.pubsub.protocols :as ppubsub]
    [audiophile.common.core.utils.logger :as log]))

(defn publish! [pubsub topic msg]
  (log/with-ctx :PubSub
    (log/trace "publishing msg to topic" topic)
    (ppubsub/publish! pubsub topic msg)
    pubsub))

(defn subscribe! [pubsub key topic listener]
  (log/with-ctx :PubSub
    (log/debug "subscribing" key "to topic" topic)
    (ppubsub/subscribe! pubsub key topic (fn [topic msg]
                                           (log/with-ctx :PubSub
                                             (log/trace "received msg for topic" topic)
                                             (listener topic msg))))
    pubsub))

(defn unsubscribe!
  ([pubsub key]
   (log/with-ctx :PubSub
     (log/debug "unsubscribing all" key)
     (ppubsub/unsubscribe! pubsub key)
     pubsub))
  ([pubsub key topic]
   (log/with-ctx :PubSub
     (log/debug "unsubscribing" key "from" topic)
     (ppubsub/unsubscribe! pubsub key topic)
     pubsub)))
