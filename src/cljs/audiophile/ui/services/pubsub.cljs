(ns audiophile.ui.services.pubsub
  (:require
    [audiophile.common.api.pubsub.protocols :as ppubsub]
    [audiophile.common.core.utils.logger :as log]
    [audiophile.common.core.utils.maps :as maps]
    [audiophile.common.infrastructure.protocols :as pcom]
    [audiophile.ui.services.pages :as pages]
    [audiophile.ui.services.ws :as ws]
    [clojure.core.async :as async]))

(defn ^:private init* [ws-init-fn pubsub subs]
  (ws-init-fn {:pubsub     pubsub
               :on-connect (fn [ch]
                             (when-let [topics (seq @subs)]
                               (async/go
                                 (async/>! ch [:conn/ping])
                                 (doseq [topic topics]
                                   (async/>! ch [:sub/start! topic])))))}))

(deftype WsPubSub [pubsub ws-init-fn subs ^:mutable ws]
  pcom/IInit
  (init! [this]
    (set! ws (init* ws-init-fn this subs)))

  pcom/IDestroy
  (destroy! [_]
    (async/go
      (doseq [topic (seq @subs)]
        (async/>! ws [:sub/stop! topic]))
      (some-> ws async/close!)
      (set! ws nil)))

  ppubsub/IPub
  (publish! [_ topic event]
    (ppubsub/publish! pubsub topic event))

  ppubsub/ISub
  (subscribe! [_ key topic listener]
    (when (= ::pages/sub key)
      (when-not ws
        (throw (ex-info "cannot subscribe before pubsub has been initialized" {})))
      (async/go
        (async/>! ws [:sub/start! topic]))
      (swap! subs conj topic))
    (ppubsub/subscribe! pubsub key topic listener))
  (unsubscribe! [_ key topic]
    (when (= ::pages/sub key)
      (when ws
        (async/go
          (async/>! ws [:sub/stop! topic])))
      (swap! subs disj topic))
    (ppubsub/unsubscribe! pubsub key topic))
  (unsubscribe! [_ key]
    (when (= ::pages/sub key)
      (when ws
        (async/go
          (doseq [topic @subs]
            (async/>! ws [:sub/stop! topic]))))
      (swap! subs empty))
    (ppubsub/unsubscribe! pubsub key)))

(defn ws [{:keys [env nav pubsub]}]
  (->WsPubSub pubsub (partial ws/init! (maps/->m env nav)) (atom #{}) nil))

(defn ws#close [pubsub]
  (pcom/destroy! pubsub))
