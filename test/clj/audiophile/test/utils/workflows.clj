(ns audiophile.test.utils.workflows
  (:require
    [audiophile.backend.api.pubsub.protocols :as pps]
    [audiophile.backend.domain.interactors.core :as int]
    [audiophile.backend.infrastructure.pubsub.handlers.workflows :as pub.workflows]
    [audiophile.common.api.pubsub.core :as pubsub]
    [audiophile.common.core.serdes.impl :as serde]
    [audiophile.common.core.utils.uuids :as uuids]
    [audiophile.common.infrastructure.pubsub.memory :as pubsub.mem]
    [audiophile.test.utils.services :as ts]
    [audiophile.test.utils.stubs :as stubs]))

(defn ^:private ->workflow [m]
  (into {}
        (map (fn [[k v]]
               [(keyword "workflow" (name k)) v]))
        m))

(defn ->mem-db
  ([db]
   (->mem-db db (constantly nil)))
  ([db query-fn]
   (swap! db empty)
   (fn [query {:keys [result-xform] :or {result-xform identity} :as opts}]
     (let [conj* (result-xform conj)]
       (or (query-fn query opts)
           (cond
             (= :workflows (:insert-into query))
             (let [updates# (map (fn [value#]
                                   (let [id# (uuids/random)]
                                     [id# (->workflow (assoc value# :id id#))]))
                                 (:values query))]
               (swap! db into updates#)
               (map (fn [[id#]]
                      {:id id#})
                    updates#))

             (:insert-into query)
             [{:id (uuids/random)}]

             (and (:select query) (= (:from query) [:workflows]))
             (conj* [] (get @db (-> query :where peek)))

             (and (:select query) (= (:from query) [:users]))
             []

             (= :workflows (:update query))
             (let [id# (-> query :where peek)]
               (swap! db update id# merge (->workflow (:set query)))
               nil)

             :else
             [{}]))))))

(defmacro with-setup [bindings & body]
  `(let [tx# (ts/->tx)
         pubsub# (pubsub.mem/pubsub {})
         commands# (reify
                     pps/IChannel
                     (send! [_# msg#]
                       (pubsub/publish! pubsub# ::amq msg#)))
         events# (ts/->chan)
         jwt-serde# (serde/jwt {:expiration 30 :secret "secret"})
         handler# (pub.workflows/->WorkflowHandler tx# {:commands  commands#
                                                        :events    events#
                                                        :jwt-serde jwt-serde#})
         db# (atom {})]
     (pubsub/subscribe! pubsub# ::commands ::amq #(int/handle! handler# %2))
     (try (let [{:keys ~bindings} {:commands  commands#
                                   :db        db#
                                   :events    events#
                                   :handler   handler#
                                   :jwt-serde jwt-serde#
                                   :pubsub    pubsub#
                                   :tx        tx#}]
            ~@body)
          (finally
            (pubsub/unsubscribe! pubsub# ::commands)))))

(defmacro with-event [[events opts] & body]
  `(let [promise# (promise)]
     (stubs/set-stub! ~events :send!
                      (fn [event#]
                        (deliver promise# event#)))
     ~@body
     (let [ms# (:timeout ~opts 1000)
           result# (deref promise# ms# ::missing)]
       (if (= ::missing result#)
         (throw (ex-info "waiting on event timed out" {:timeout ms#}))
         result#))))
