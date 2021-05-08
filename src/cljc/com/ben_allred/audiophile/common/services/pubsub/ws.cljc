(ns com.ben-allred.audiophile.common.services.pubsub.ws
  (:require
    [clojure.core.async :as async]
    [clojure.core.match :as match]
    [com.ben-allred.audiophile.common.services.navigation.core :as nav]
    [com.ben-allred.audiophile.common.services.serdes.core :as serdes]
    [com.ben-allred.audiophile.common.services.ui-store.core :as ui-store]
    [com.ben-allred.audiophile.common.utils.logger :as log]
    [com.ben-allred.audiophile.common.utils.uri :as uri]
    [com.ben-allred.vow.core :as v #?@(:cljs [:include-macros true])]
    [com.ben-allred.ws-client-cljc.core :as ws*]
    [integrant.core :as ig]))

(defn handle-msg [store msg]
  (some-> msg
          (match/match
            [msg-type event-id data] [:ws/message [msg-type {:id   event-id
                                                             :data data}]]
            [msg-type event-id data ctx] [:ws/message [msg-type {:id   event-id
                                                                 :data data
                                                                 :ctx  ctx}]]
            event (log/warn "unknown msg" event))
          (->> (ui-store/dispatch! store))))

(defn ws-uri [nav serde base-url]
  (let [params {:query-params {:content-type (serdes/mime-type serde)}}]
    (-> base-url
        str
        uri/parse
        (assoc :path nil :query nil :fragment nil)
        (update :scheme {"http" "ws" "https" "wss"})
        uri/stringify
        (str (nav/path-for nav :api/ws params)))))

(defmethod ig/init-key ::handler [_ {:keys [base-url nav reconnect-ms serde store user-details]}]
  (let [url (ws-uri nav serde #?(:cljs (.-location js/window) :default base-url))
        vol (volatile! nil)]
    (-> user-details
        (v/then (fn [details]
                  (when (and details (nil? @vol))
                    (let [ws (ws*/keep-alive! url
                                              {:reconnect-ms reconnect-ms
                                               :in-buf-or-n  100
                                               :in-xform     (comp (map (partial serdes/deserialize serde))
                                                                   (remove (comp #{:conn/ping :conn/pong} first)))
                                               :out-buf-or-n 100
                                               :out-xform    (map (partial serdes/serialize serde))})]
                      (vreset! vol ws)
                      (async/go-loop []
                        (when-let [msg (async/<! ws)]
                          (handle-msg store msg)
                          (recur))))))))
    vol))

(defmethod ig/halt-key! ::handler [_ ws]
  (some-> ws deref async/close!)
  (vreset! ws ::disco))
