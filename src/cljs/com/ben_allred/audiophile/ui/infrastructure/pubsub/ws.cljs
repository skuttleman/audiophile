(ns com.ben-allred.audiophile.ui.infrastructure.pubsub.ws
  (:require
    [clojure.core.async :as async]
    [clojure.core.match :as match]
    [com.ben-allred.audiophile.common.core.navigation.core :as nav]
    [com.ben-allred.audiophile.common.core.serdes.core :as serdes]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.common.core.utils.uri :as uri]
    [com.ben-allred.audiophile.ui.infrastructure.store.core :as store]
    [com.ben-allred.vow.core :as v :include-macros true]
    [com.ben-allred.ws-client-cljc.core :as ws*]))

(defn handle-msg [store msg]
  (some-> msg
          (match/match
            [msg-type event-id data] [:ws/message [msg-type {:id   event-id
                                                             :data data}]]
            [msg-type event-id data ctx] [:ws/message [msg-type {:id   event-id
                                                                 :data data
                                                                 :ctx  ctx}]]
            event (log/warn "unknown msg" event))
          (->> (store/dispatch! store))))

(defn ws-uri [nav serde base-url]
  (let [mime-type (serdes/mime-type serde)
        params {:query-params {:content-type mime-type
                               :accept       mime-type}}]
    (-> base-url
        str
        uri/parse
        (assoc :path nil :query nil :fragment nil)
        (update :scheme {"http" "ws" "https" "wss"})
        uri/stringify
        (str (nav/path-for nav :ws/connection params)))))

(defn client [{:keys [env nav reconnect-ms serde store]}]
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
            (handle-msg store msg)
            (recur)))
        ws))))

(defn client#close [ws]
  (some-> ws async/close!))
