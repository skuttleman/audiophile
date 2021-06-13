(ns com.ben-allred.audiophile.common.infrastructure.pubsub.core
  (:require
    [com.ben-allred.audiophile.common.infrastructure.pubsub.protocols :as ppubsub]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]))

(defn publish! [pubsub topic event]
  (ppubsub/publish! pubsub topic event)
  pubsub)

(defn subscribe! [pubsub key topic listener]
  (ppubsub/subscribe! pubsub key topic listener)
  pubsub)

(defn unsubscribe!
  ([pubsub key]
   (ppubsub/unsubscribe! pubsub key)
   pubsub)
  ([pubsub key topic]
   (ppubsub/unsubscribe! pubsub key topic)
   pubsub))
