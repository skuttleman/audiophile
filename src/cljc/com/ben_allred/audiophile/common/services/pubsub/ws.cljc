(ns com.ben-allred.audiophile.common.services.pubsub.ws
  (:require
    [clojure.core.async :as async]
    [clojure.core.match :as match]
    [com.ben-allred.audiophile.common.services.navigation.core :as nav]
    [com.ben-allred.audiophile.common.services.serdes.core :as serdes]
    [com.ben-allred.audiophile.common.utils.logger :as log]
    [com.ben-allred.ws-client-cljc.core :as ws*]
    [integrant.core :as ig]
    [com.ben-allred.audiophile.common.services.ui-store.core :as ui-store]))

(defmethod ig/init-key ::ws-ch [_ {:keys [nav serde]}]
  (ws*/keep-alive! #_(nav/path-for nav :api/ws {:query-params {:content-type (serdes/mime-type serde)}})
    "ws://localhost:3000/api/ws?content-type=application/edn"
    {:in-buf-or-n  100
     :in-xform     (comp (map (partial serdes/deserialize serde))
                         (remove (comp #{:conn/ping :conn/pong} first)))
     :out-buf-or-n 100
     :out-xform    (map (partial serdes/serialize serde))}))

(defmethod ig/halt-key! ::ws-ch [_ ch]
  (async/close! ch))

(defn ^:private handle-msg [store msg]
  (match/match msg
    [msg-type event-id data] (ui-store/dispatch! store
                                                 [:ws/message {:type msg-type
                                                               :id   event-id
                                                               :data data}])
    [msg-type event-id data ctx] (ui-store/dispatch! store
                                                     [:ws/message {:type msg-type
                                                                   :id   event-id
                                                                   :ctx  ctx
                                                                   :data data}])
    msg (log/warn "unknown msg" msg)))

(defmethod ig/init-key ::handler [_ {:keys [store ws-ch]}]
  (async/go-loop []
    (when-let [msg (async/<! ws-ch)]
      (handle-msg store msg)
      (recur))))
