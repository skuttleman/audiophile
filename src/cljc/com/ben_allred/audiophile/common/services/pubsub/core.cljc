(ns com.ben-allred.audiophile.common.services.pubsub.core
  (:require
    [com.ben-allred.audiophile.common.services.pubsub.protocols :as ppubsub]
    [com.ben-allred.audiophile.common.utils.logger :as log]
    [integrant.core :as ig]))

(defn ^:private publish* [state this topic event]
  (let [state (log/spy :warn @state)
        subs (log/spy :warn (get-in state [:subs topic]))]
    (doseq [key subs
            :let [handler (log/spy :warn (get-in state [:listeners key topic]))]
            :when handler]
      (try (handler topic event)
           (catch #?(:cljs :default :default Throwable) _
             (log/spy :warn _)
             (ppubsub/unsubscribe! this key))))))

(defn subscribe* [state key topic listener]
  (swap! state (fn [state]
                 (-> state
                     (assoc-in [:listeners key topic] listener)
                     (update-in [:topics key] (fnil conj #{}) topic)
                     (update-in [:subs topic] (fnil conj #{}) key)))))

(defn ^:private unsubscribe*
  ([state key]
   (swap! state (fn [state]
                  (-> state
                      (update :listeners dissoc key)
                      (update :subs (fn [subs]
                                      (persistent! (reduce (fn [subs' topic]
                                                             (let [set (get subs topic)]
                                                               (if-let [next-set (not-empty (disj set key))]
                                                                 (assoc! subs' topic next-set)
                                                                 (dissoc! subs' topic))))
                                                           (transient subs)
                                                           (get-in state [:topics key])))))
                      (update :topics dissoc key)))))
  ([state key topic]
   (swap! state (fn [state]
                  (let [next-subs (not-empty (disj (get-in state [:subs topic]) key))
                        next-topics (not-empty (disj (get-in state [:topics key])))]
                    (-> state
                        (cond->
                          next-subs (assoc-in [:subs topic] next-subs)
                          (not next-subs) (update :subs dissoc topic)
                          next-topics (assoc-in [:topics key] next-topics)
                          (not next-topics) (-> (update :topics dissoc key)
                                                (update :listeners dissoc key)))))))))

(deftype PubSub [state]
  ppubsub/IPubSub
  (publish! [this topic event]
    (publish* state this topic event)
    this)
  (subscribe! [this key topic listener]
    (subscribe* state key topic listener)
    this)
  (unsubscribe! [this key]
    (unsubscribe* state key)
    this)
  (unsubscribe! [this key topic]
    (unsubscribe* state key topic)
    this))

(defmethod ig/init-key ::pubsub [_ _]
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