(ns com.ben-allred.audiophile.common.infrastructure.pubsub.core
  (:require
    [com.ben-allred.audiophile.common.infrastructure.pubsub.protocols :as ppubsub]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]))

(defn publish! [pubsub topic msg]
  (log/debug "[PubSub] publishing msg to topic" topic)
  (ppubsub/publish! pubsub topic msg)
  pubsub)

(defn subscribe! [pubsub key topic listener]
  (log/debug "[PubSub] subscribing" key "to topic" topic)
  (ppubsub/subscribe! pubsub key topic (fn [topic msg]
                                         (log/debug "[PubSub] received msg for topic" topic)
                                         (listener topic msg)))
  pubsub)

(defn unsubscribe!
  ([pubsub key]
   (log/debug "[PubSub] unsubscribing all" key)
   (ppubsub/unsubscribe! pubsub key)
   pubsub)
  ([pubsub key topic]
   (log/debug "[PubSub] unsubscribing" key "from" topic)
   (ppubsub/unsubscribe! pubsub key topic)
   pubsub))
