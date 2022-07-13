(ns audiophile.ui.services.pubsub
  (:require
    [audiophile.common.api.pubsub.core :as pubsub]
    [audiophile.common.api.pubsub.protocols :as ppubsub]
    [audiophile.ui.services.ws :as ws]
    [clojure.core.async :as async]))

(deftype WsPubSub [pubsub]
  ppubsub/IPub
  (publish! [_ topic event]
    (ppubsub/publish! pubsub topic event))

  ppubsub/ISub
  (subscribe! [_ key topic listener]
    (ppubsub/subscribe! pubsub key topic listener)
    (async/go
      (async/>! @ws/conn [:sub/start! topic])))
  (unsubscribe! [_ _]
    (throw (ex-info "unsupported operation" {})))
  (unsubscribe! [_ key topic]
    (async/go
      (async/>! @ws/conn [:sub/stop! topic]))
    (pubsub/unsubscribe! pubsub key topic)))

(defn ws [{:keys [pubsub]}]
  (->WsPubSub pubsub))
