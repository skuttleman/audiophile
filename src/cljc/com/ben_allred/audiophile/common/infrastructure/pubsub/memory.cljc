(ns com.ben-allred.audiophile.common.infrastructure.pubsub.memory
  (:require
    [clojure.core.async :as async]
    [com.ben-allred.audiophile.common.api.pubsub.protocols :as ppubsub]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.common.core.utils.maps :as maps]
    [com.ben-allred.audiophile.common.infrastructure.http.protocols :as phttp]))

(defn ^:private publish* [state topic event]
  (let [state @state
        subs (get-in state [:subs topic])]
    (async/go-loop [[key :as subs] (seq subs)]
      (when (seq subs)
        (when-let [handler (get-in state [:listeners key topic])]
          (async/>! handler [topic event]))
        (recur (rest subs))))))

(defn ^:private subscribe* [state key topic listener]
  (when (get-in @state [:listeners key topic])
    (throw (ex-info "key is already associated with topic" (maps/->m key topic))))
  (let [ch (async/chan 100)]
    (async/go-loop []
      (when-let [[topic event] (async/<! ch)]
        (listener topic event)
        (recur)))
    (swap! state (fn [state]
                   (-> state
                       (assoc-in [:listeners key topic] ch)
                       (update-in [:topics key] (fnil conj #{}) topic)
                       (update-in [:subs topic] (fnil conj #{}) key))))))

(defn ^:private unsubscribe-all [state key]
  (swap! state (fn [state]
                 (-> state
                     (update :listeners dissoc key)
                     (update :topics dissoc key)
                     (update :subs (fn [subs]
                                     (persistent! (reduce (fn [subs' topic]
                                                            (some-> state
                                                                    (get-in [:listeners key topic])
                                                                    async/close!)
                                                            (let [set (get subs topic)]
                                                              (if-let [next-set (not-empty (disj set key))]
                                                                (assoc! subs' topic next-set)
                                                                (dissoc! subs' topic))))
                                                          (transient subs)
                                                          (get-in state [:topics key])))))))))

(defn ^:private unsubscribe* [state key topic]
  (swap! state (fn [state]
                 (some-> state
                         (get-in [:listeners key topic])
                         async/close!)
                 (let [next-subs (not-empty (disj (get-in state [:subs topic]) key))
                       next-topics (not-empty (disj (get-in state [:topics key])))]
                   (-> state
                       (cond->
                         next-subs (assoc-in [:subs topic] next-subs)
                         (not next-subs) (update :subs dissoc topic)
                         next-topics (assoc-in [:topics key] next-topics)
                         (not next-topics) (-> (update :topics dissoc key)
                                               (update :listeners dissoc key))))))))

(deftype MemoryPubSub [state]
  ppubsub/IPub
  (publish! [_ topic event]
    (publish* state topic event))

  ppubsub/ISub
  (subscribe! [_ key topic listener]
    (subscribe* state key topic listener))
  (unsubscribe! [_ key]
    (unsubscribe-all state key))
  (unsubscribe! [_ key topic]
    (unsubscribe* state key topic))

  phttp/ICheckHealth
  (display-name [_]
    ::MemoryPubSub)
  (healthy? [_]
    true)
  (details [_]
    {:connections (count (:listeners @state))}))

(defn pubsub
  "Constructor for [[PubSub]] which provides an in memory pub/sub protocol."
  [_]
  (->MemoryPubSub (atom nil)))
