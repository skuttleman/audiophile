(ns com.ben-allred.audiophile.common.infrastructure.pubsub.core
  (:require
    [com.ben-allred.audiophile.common.infrastructure.pubsub.protocols :as ppubsub]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]))

(defn ^:private publish* [state this topic event]
  (let [state @state
        subs (get-in state [:subs topic])]
    (doseq [key subs
            :let [handler (get-in state [:listeners key topic])]
            :when handler]
      (try (handler topic event)
           (catch #?(:cljs :default :default Throwable) _
             (ppubsub/unsubscribe! this key topic))))))

(defn subscribe* [state key topic listener]
  (swap! state (fn [state]
                 (-> state
                     (assoc-in [:listeners key topic] listener)
                     (update-in [:topics key] (fnil conj #{}) topic)
                     (update-in [:subs topic] (fnil conj #{}) key)))))

(defn ^:private unsubscribe-all [state key]
  (swap! state (fn [state]
                 (-> state
                     (update :listeners dissoc key)
                     (update :topics dissoc key)
                     (update :subs (fn [subs]
                                     (persistent! (reduce (fn [subs' topic]
                                                            (let [set (get subs topic)]
                                                              (if-let [next-set (not-empty (disj set key))]
                                                                (assoc! subs' topic next-set)
                                                                (dissoc! subs' topic))))
                                                          (transient subs)
                                                          (get-in state [:topics key])))))))))

(defn ^:private unsubscribe* [state key topic]
  (swap! state (fn [state]
                 (let [next-subs (not-empty (disj (get-in state [:subs topic]) key))
                       next-topics (not-empty (disj (get-in state [:topics key])))]
                   (-> state
                       (cond->
                         next-subs (assoc-in [:subs topic] next-subs)
                         (not next-subs) (update :subs dissoc topic)
                         next-topics (assoc-in [:topics key] next-topics)
                         (not next-topics) (-> (update :topics dissoc key)
                                               (update :listeners dissoc key))))))))

(deftype PubSub [state]
  ppubsub/IPubSub
  (publish! [this topic event]
    (publish* state this topic event)
    this)
  (subscribe! [this key topic listener]
    (subscribe* state key topic listener)
    this)
  (unsubscribe! [this key]
    (unsubscribe-all state key)
    this)
  (unsubscribe! [this key topic]
    (unsubscribe* state key topic)
    this))

(defn pubsub [_]
  (->PubSub (atom nil)))

(defn publish! [pubsub topic event]
  (ppubsub/publish! pubsub topic event))

(defn subscribe! [pubsub key topic listener]
  (ppubsub/subscribe! pubsub key topic listener))

(defn unsubscribe!
  ([pubsub key]
   (ppubsub/unsubscribe! pubsub key))
  ([pubsub key topic]
   (ppubsub/unsubscribe! pubsub key topic)))
