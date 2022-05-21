(ns com.ben-allred.audiophile.ui.infrastructure.services.ws
  (:require
    [clojure.core.async :as async]
    [clojure.core.match :as match]
    [com.ben-allred.audiophile.common.core.serdes.core :as serdes]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.common.core.utils.uri :as uri]
    [com.ben-allred.audiophile.common.api.pubsub.core :as pubsub]
    [com.ben-allred.ws-client-cljc.core :as ws*]
    [com.ben-allred.audiophile.common.infrastructure.navigation.core :as nav]
    [com.ben-allred.audiophile.common.core.serdes.impl :as serde]))

(defn handle-msg [pubsub msg]
  (when-let [event (when msg
                     (match/match msg
                       [msg-type event-id data ctx] {:id   event-id
                                                     :data data
                                                     :ctx  ctx}
                       _ nil))]
    (let [{request-id :request/id} (:ctx event)
          [topic event] (case (get-in event [:data :event/type])
                          :command/failed [request-id {:error [(get-in event [:data :event/data])]}]
                          [request-id {:data (get-in event [:data :event/data])}])]
      (when topic
        (log/trace "[WS msg]" topic event (:ctx event))
        (pubsub/publish! pubsub topic event)))))

(defn ws-uri [nav base-url]
  (let [mime-type (serdes/mime-type serde/transit)
        params {:params {:content-type mime-type
                         :accept       mime-type}}]
    (-> base-url
        str
        uri/parse
        (assoc :path nil :query nil :fragment nil)
        (update :scheme {"https" "wss"} "ws")
        uri/stringify
        (str (nav/path-for nav :ws/connection params)))))

(defn client [{:keys [env nav pubsub reconnect-ms store]}]
  (let [url (ws-uri nav (:api-base env))]
    (when (:user/profile @store)
      (let [ws (ws*/keep-alive! url
                                {:reconnect-ms (or reconnect-ms 100)
                                 :in-buf-or-n  100
                                 :in-xform     (comp (map (partial serdes/deserialize serde/transit))
                                                     (remove (comp #{:conn/ping :conn/pong} first)))
                                 :out-buf-or-n 100
                                 :out-xform    (map (partial serdes/serialize serde/transit))})]
        (async/go-loop []
          (when-let [msg (async/<! ws)]
            (handle-msg pubsub msg)
            (recur)))
        ws))))

(defn client#close [ws]
  (some-> ws async/close!))
