(ns audiophile.ui.services.ws
  (:require
    [audiophile.common.api.pubsub.core :as pubsub]
    [audiophile.common.core.serdes.core :as serdes]
    [audiophile.common.core.serdes.impl :as serde]
    [audiophile.common.core.utils.logger :as log]
    [audiophile.common.core.utils.uri :as uri]
    [audiophile.common.infrastructure.navigation.core :as nav]
    [clojure.core.async :as async]
    [clojure.core.match :as match]
    [com.ben-allred.ws-client-cljc.core :as ws*]))

(defn ^:private handle-msg [pubsub msg]
  (when-let [[event-type event ctx] (match/match msg
                                      [event-type _ data ctx] [event-type data ctx]
                                      _ nil)]
    (log/info "websocket msg received" event-type ctx)
    (let [{:event/keys [data type]} event
          msg' (merge {:event/type type}
                      (case type
                        :workflow/failed {:error [data]}
                        {:data data}))]
      (doseq [topic [(:request/id ctx) event-type]
              :when topic]
        (pubsub/publish! pubsub topic msg')))))

(defn ^:private ws-uri [nav base-url]
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

(defn init! [{:keys [env nav]} {:keys [on-connect pubsub]}]
  (let [url (ws-uri nav (:api-base env))]
    (let [ws (ws*/keep-alive! url
                              {:reconnect-ms 100
                               :in-buf-or-n  100
                               :in-xform     (comp (map (partial serdes/deserialize serde/transit))
                                                   (remove nil?)
                                                   (remove (comp #{:conn/ping :conn/pong} first)))
                               :out-buf-or-n 100
                               :out-xform    (map (partial serdes/serialize serde/transit))
                               :on-connect   on-connect})]
      (async/go-loop []
        (when-let [msg (async/<! ws)]
          (handle-msg pubsub msg)
          (recur)))
      ws)))
