(ns com.ben-allred.audiophile.integration.common.components
  (:require
    [clojure.core.async :as async]
    [clojure.core.async.impl.protocols :as async.protocols]
    [com.ben-allred.audiophile.api.services.pubsub.ws :as ws]
    [com.ben-allred.audiophile.common.services.serdes.core :as serdes]
    [com.ben-allred.audiophile.common.utils.http :as http]
    [com.ben-allred.audiophile.common.utils.logger :as log]
    [integrant.core :as ig])
  (:import
    (org.projectodd.wunderboss.web.async Channel)))

(defmethod ig/init-key ::ws-handler [_ {:keys [->channel ->handler serdes]}]
  (fn [request]
    (when (:websocket? request)
      (let [params (get-in request [:nav/route :query-params])
            serializer (serdes/find-serde serdes
                                          (or (:content-type params)
                                              (:accept params)
                                              ""))
            deserializer (serdes/find-serde serdes
                                            (or (:accept params)
                                                (:content-type params)
                                                ""))
            msgs (async/chan 100)
            channel (->channel request
                               (reify ws/IChannel
                                 (open? [_]
                                   (not (async.protocols/closed? msgs)))
                                 (send! [_ msg]
                                   (async/>!! msgs (serdes/deserialize deserializer msg)))
                                 (close! [_]
                                   (async/close! msgs))))
            handler (->handler request channel)]
        (ws/on-open handler)
        [::http/ok (reify
                     Channel

                     async.protocols/ReadPort
                     (take! [_ fn1-handler]
                       (async.protocols/take! msgs fn1-handler))

                     async.protocols/WritePort
                     (put! [_ val _]
                       (if (ws/open? channel)
                         (do (ws/on-message handler (serdes/serialize serializer val))
                             (delay true))
                         (delay false)))

                     async.protocols/Channel
                     (closed? [_]
                       (not (ws/open? channel)))
                     (close! [_]
                       (when (ws/open? channel)
                         (ws/on-close handler))
                       (ws/close! channel)))]))))
