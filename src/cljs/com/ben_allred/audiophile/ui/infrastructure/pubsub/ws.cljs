(ns com.ben-allred.audiophile.ui.infrastructure.pubsub.ws
  (:require
    [clojure.core.async :as async]
    [clojure.core.match :as match]
    [com.ben-allred.audiophile.common.api.navigation.core :as nav]
    [com.ben-allred.audiophile.common.core.serdes.core :as serdes]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.common.core.utils.uri :as uri]
    [com.ben-allred.audiophile.common.api.pubsub.core :as pubsub]
    [com.ben-allred.audiophile.ui.infrastructure.store.core :as store]
    [com.ben-allred.ws-client-cljc.core :as ws*]))

(defn handle-msg [pubsub store msg]
  (when-let [[msg-type event] (when msg
                                (match/match msg
                                  [msg-type event-id data ctx] [msg-type {:id   event-id
                                                                          :data data
                                                                          :ctx  ctx}]
                                  _ nil))]
    (let [{request-id :request/id} (:ctx event)
          [topic event] (case (get-in event [:data :event/type])
                          :command/failed [request-id {:error [(get-in event [:data :event/data])]}]
                          [request-id {:data (get-in event [:data :event/data])}])]
      (when topic
        (log/trace "[WS msg]" topic event (:ctx event))
        (pubsub/publish! pubsub topic event)))
    (store/dispatch! store [:ws/message [msg-type event]])))

(defn ws-uri [nav serde base-url]
  (let [mime-type (serdes/mime-type serde)
        params {:params {:content-type mime-type
                         :accept       mime-type}}]
    (-> base-url
        str
        uri/parse
        (assoc :path nil :query nil :fragment nil)
        (update :scheme {"https" "wss"} "ws")
        uri/stringify
        (str (nav/path-for nav :ws/connection params)))))

(defn client [{:keys [env nav pubsub reconnect-ms serde store]}]
  (let [url (ws-uri nav serde (:api-base env))
        {:auth/keys [user]} (store/get-state store)]
    (when user
      (let [ws (ws*/keep-alive! url
                                {:reconnect-ms reconnect-ms
                                 :in-buf-or-n  100
                                 :in-xform     (comp (map (partial serdes/deserialize serde))
                                                     (remove (comp #{:conn/ping :conn/pong} first)))
                                 :out-buf-or-n 100
                                 :out-xform    (map (partial serdes/serialize serde))})]
        (async/go-loop []
          (when-let [msg (async/<! ws)]
            (handle-msg pubsub store msg)
            (recur)))
        ws))))

(defn client#close [ws]
  (some-> ws async/close!))
