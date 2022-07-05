(ns audiophile.ui.services.ws
  (:require
    [clojure.core.async :as async]
    [clojure.core.match :as match]
    [audiophile.common.api.pubsub.core :as pubsub]
    [audiophile.common.core.serdes.core :as serdes]
    [audiophile.common.core.serdes.impl :as serde]
    [audiophile.common.core.utils.logger :as log]
    [audiophile.common.core.utils.uri :as uri]
    [audiophile.common.infrastructure.navigation.core :as nav]
    [com.ben-allred.ws-client-cljc.core :as ws*]))

(defonce ^:private conn
  (atom nil))

(defn handle-msg [pubsub msg]
  (let [event (match/match msg
                     [msg-type event-id data ctx] {:id   event-id
                                                   :data data
                                                   :ctx  ctx}
                     _ nil)
        event-type (get-in event [:data :event/type])
        event-data (get-in event [:data :event/data])]
    (when-let [request-id (-> event :ctx :request/id)]
      (let [event* (case event-type
                     :workflow/failed {:error [event-data]}
                     {:data event-data})]
        (log/info [event-type (:ctx event)])
        (pubsub/publish! pubsub request-id event*)))))

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
        (str (nav/path-for nav :routes.ws/connection params)))))

(defn init! [{:keys [env nav pubsub]}]
  (let [url (ws-uri nav (:api-base env))]
    (some-> @conn async/close!)
    (let [ws (ws*/keep-alive! url
                              {:reconnect-ms 100
                               :in-buf-or-n  100
                               :in-xform     (comp (map (partial serdes/deserialize serde/transit))
                                                   (remove nil?)
                                                   (remove (comp #{:conn/ping :conn/pong} first)))
                               :out-buf-or-n 100
                               :out-xform    (map (partial serdes/serialize serde/transit))})]
      (reset! conn ws)
      (async/go-loop []
        (when-let [msg (async/<! ws)]
          (handle-msg pubsub msg)
          (recur)))
      nil)))
