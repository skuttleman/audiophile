(ns audiophile.backend.infrastructure.workflows.core
  (:require
    [audiophile.backend.api.pubsub.core :as ps]
    [audiophile.backend.api.pubsub.protocols :as pps]
    [audiophile.backend.infrastructure.workflows.handlers :as wfh]
    [audiophile.common.core.utils.core :as u]
    [audiophile.common.core.utils.logger :as log]
    [audiophile.common.core.utils.maps :as maps]
    [audiophile.common.core.utils.uuids :as uuids]
    [clojure.core.async :as async]
    [kinsky.client :as client*]
    [spigot.context :as sp.ctx]
    [spigot.controllers.kafka.common :as sp.kcom]
    [spigot.controllers.kafka.core :as sp.kafka]
    [spigot.controllers.protocols :as sp.pcon])
  (:import
    (java.lang AutoCloseable)
    (java.io Closeable)))

(defn ^:private extract-result [workflow-id data]
  (-> data
      :workflows/->result
      (sp.ctx/resolve-params (:ctx data))
      (assoc :workflow/id workflow-id
             :workflow/template (:workflows/template data))))

(deftype SpigotHandler [sys]
  sp.pcon/ISpigotTaskHandler
  (on-error [this ctx ex]
    (log/with-ctx [this :WF]
      (log/error ex "workflow failed" ctx)
      (let [data (maps/assoc-maybe {}
                                   :error/reason (ex-message ex)
                                   :error/details (ex-data ex))]
        (ps/generate-event (:workflow/id ctx) :command/failed data ctx))))
  (on-complete [this {workflow-id :workflow/id :as ctx} workflow]
    (log/with-ctx [this :WF]
      (log/info "workflow succeeded" ctx)
      (ps/generate-event workflow-id
                         :workflow/completed
                         (extract-result workflow-id workflow)
                         ctx)))
  (process-task [this ctx task]
    (log/with-ctx [this :WF]
      (log/info "processing task" (select-keys task #{:spigot/id :spigot/tag}))
      (wfh/task-handler sys ctx task))))

(deftype SpigotProducer [kinsky-producer topic-cfg ^:volatile-mutable open?]
  pps/IChannel
  (open? [_]
    open?)
  (send! [_ record]
    @(client*/send! kinsky-producer (assoc record :topic (:name topic-cfg))))
  (close! [_]
    (u/silent!
      (set! open? false)
      (.close ^Closeable @kinsky-producer))))

(defn handler [sys]
  (->SpigotHandler sys))

(defn consumer [{:keys [cfg listener timeout topic-cfg]}]
  (let [cfg (maps/assoc-defaults cfg :group.id (str (uuids/random)))
        client (client*/consumer cfg
                                 (.deserializer (:key-serde topic-cfg))
                                 (.deserializer (:val-serde topic-cfg)))
        polling? (volatile! true)]
    (client*/subscribe! client (:name topic-cfg))
    (async/go-loop []
      (when-let [{:keys [by-topic]} (when @polling?
                                      (u/silent! (client*/poll! client (or timeout 1000))))]
        (u/silent! (->> by-topic
                        (mapcat val)
                        (run! listener))
                   (client*/commit! client))
        (async/<! (async/timeout 100))
        (recur)))
    (reify
      Closeable
      (close [_]
        (vreset! polling? false)
        (u/silent! (.close ^Closeable @client))))))

(defn consumer#close [^Closeable client]
  (.close client))

(defn producer [{:keys [cfg topic-cfg]}]
  (->SpigotProducer (client*/producer cfg)
                    topic-cfg
                    true))

(defn producer#close [client]
  (u/silent! (ps/close! client)))

(defn topic-cfg [{:keys [name] :as cfg}]
  (merge cfg (sp.kcom/->topic-cfg name)))

(defn wf-controller [{:keys [cfg] :as opts}]
  (sp.kafka/start! cfg opts))

(defn wf-controller#close [^AutoCloseable controller]
  (.close controller))
