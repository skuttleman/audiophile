(ns com.ben-allred.audiophile.backend.infrastructure.pubsub.core
  (:require
    [com.ben-allred.audiophile.backend.infrastructure.pubsub.protocols :as pps]))

(defn open? [ch]
  (pps/open? ch))

(defn send! [ch msg]
  (pps/send! ch msg)
  nil)

(defn close! [ch]
  (pps/close! ch)
  nil)

(defn on-open [handler]
  (pps/on-open handler))

(defn on-message [handler msg]
  (pps/on-message handler msg))

(defn on-close [handler]
  (pps/on-close handler))

(defn chan [conn opts]
  (pps/chan conn opts))

(defn subscribe! [ch handler opts]
  (pps/subscribe! ch handler opts)
  nil)
