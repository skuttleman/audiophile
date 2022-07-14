(ns audiophile.ui.services.pubsub
  (:require
    [audiophile.common.api.pubsub.core :as pubsub]
    [audiophile.common.api.pubsub.protocols :as ppubsub]
    [audiophile.common.core.utils.maps :as maps]
    [audiophile.common.infrastructure.protocols :as pcom]
    [audiophile.ui.services.ws :as ws]
    [clojure.core.async :as async]))

(deftype WsPubSub [env nav pubsub ^:mutable ws]
  pcom/IInit
  (init! [this]
    (set! ws (ws/init! (maps/->m {:pubsub this} env nav))))

  pcom/IDestroy
  (destroy! [_]
    (some-> ws async/close!)
    (set! ws nil))

  ppubsub/IPub
  (publish! [_ topic event]
    (pubsub/publish! pubsub topic event))

  ppubsub/ISub
  (subscribe! [_ key topic listener]
    (when-not ws
      (throw (ex-info "cannot subscribe before pubsub has been initialized" {})))
    (pubsub/subscribe! pubsub key topic listener)
    (async/go
      (async/>! ws [:sub/start! topic])))
  (unsubscribe! [_ key topic]
    (pubsub/unsubscribe! pubsub key topic)
    (when ws
      (async/go
        (async/>! ws [:sub/stop! topic])))))

(defn ws [{:keys [env nav pubsub]}]
  (->WsPubSub env nav pubsub nil))

(defn ws#close [pubsub]
  (pcom/destroy! pubsub))
