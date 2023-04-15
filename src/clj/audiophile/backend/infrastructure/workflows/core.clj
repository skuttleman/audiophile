(ns audiophile.backend.infrastructure.workflows.core
  (:require
    [audiophile.backend.api.pubsub.core :as ps]
    [audiophile.backend.api.pubsub.protocols :as pps]
    [audiophile.backend.infrastructure.workflows.handlers :as wfh]
    [audiophile.common.core.utils.core :as u]
    [audiophile.common.core.utils.logger :as log]
    [audiophile.common.core.utils.maps :as maps]
    [audiophile.common.core.utils.uuids :as uuids]
    [audiophile.common.infrastructure.http.protocols :as phttp]
    [clojure.core.async :as async]
    [kinsky.client :as client*]
    [spigot.controllers.kafka.common :as sp.kcom]
    [spigot.controllers.kafka.core :as sp.kafka]
    [spigot.controllers.protocols :as sp.pcon]
    [spigot.impl.api :as spapi])
  (:import
    (java.io Closeable)))

(defn ^:private generate-event [model-id event-type data {user-id :user/id :as ctx}]
  {:event/id         (uuids/random)
   :event/model-id   model-id
   :event/type       event-type
   :event/data       data
   :event/emitted-by user-id
   :event/ctx        ctx})

(deftype SpigotStatusHandler []
  sp.pcon/IWorkflowHandler
  (on-create [this {workflow-id :workflow/id :as ctx} workflow]
    (log/with-ctx (merge ctx {:logger/id :WF :logger/class this})
      (log/debug "workflow created" ctx)
      (generate-event workflow-id :workflow/created workflow ctx)))
  (on-update [this {workflow-id :workflow/id :as ctx} workflow]
    (log/with-ctx (merge ctx {:logger/id :WF :logger/class this})
      (log/debug "workflow updated" ctx)
      (generate-event workflow-id :workflow/updated workflow ctx)))
  (on-complete [this {workflow-id :workflow/id :as ctx} workflow]
    (log/with-ctx (merge ctx {:logger/id :WF :logger/class this})
      (log/info "workflow succeeded" ctx)
      (generate-event workflow-id :workflow/completed workflow ctx)))

  sp.pcon/IErrorHandler
  (on-error [this {workflow-id :workflow/id :as ctx} workflow]
    (log/with-ctx (merge ctx {:logger/id :WF :logger/class this})
      (let [error (spapi/error workflow)]
        (log/error "workflow failed" error ctx)
        (let [data {:workflow/template (:workflows/template workflow)
                    :error/details     error}]
          (generate-event workflow-id :workflow/failed data ctx))))))

(deftype SpigotHandler [sys status-handler]
  sp.pcon/IWorkflowHandler
  (on-create [_ ctx workflow]
    (sp.pcon/on-create status-handler ctx workflow))
  (on-update [_ ctx workflow]
    (sp.pcon/on-update status-handler ctx workflow))
  (on-complete [_ ctx workflow]
    (sp.pcon/on-complete status-handler ctx workflow))

  sp.pcon/IErrorHandler
  (on-error [_ ctx workflow]
    (sp.pcon/on-error status-handler ctx workflow))

  sp.pcon/ITaskProcessor
  (process-task [this ctx task]
    (log/with-ctx (merge ctx {:logger/id :WF :logger/class this})
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

(deftype SpigotKafkaController [kafka-streams]
  phttp/ICheckHealth
  (display-name [_]
    ::SpigotKafkaController)
  (healthy? [_]
    (sp.kafka/running? kafka-streams))
  (details [_]
    nil)

  Closeable
  (close [_]
    (u/silent!
      (sp.kafka/stop! kafka-streams))))

(defn handler [{:keys [status-handler sys]}]
  (->SpigotHandler sys status-handler))

(defn status-handler [_]
  (->SpigotStatusHandler))

(deftype SpigotKafkaConsumer [id ^Closeable client topic-cfg polling?]
  phttp/ICheckHealth
  (display-name [_]
    [::SpigotKafkaConsumer id])
  (healthy? [_]
    @polling?)
  (details [_]
    {:topic (:name topic-cfg)})

  Closeable
  (close [_]
    (vreset! polling? false)
    (u/silent! (.close client))))

(defn consumer [{:keys [cfg id listener timeout topic-cfg]}]
  (let [cfg (maps/assoc-defaults cfg :group.id (str (uuids/random)))
        client (client*/consumer cfg
                                 (.deserializer (:key-serde topic-cfg))
                                 (.deserializer (:val-serde topic-cfg)))
        polling? (volatile! true)]
    (client*/subscribe! client (:name topic-cfg))
    (async/go-loop []
      (when-let [{:keys [by-topic]} (if @polling?
                                      (u/silent! (client*/poll! client (or timeout 1000)))
                                      (log/debug "shutting down consumer" id))]
        (try (->> by-topic
                  (mapcat val)
                  (run! listener))
             (client*/commit! client)
             (catch Throwable ex
               (log/error ex "consumer error" id)
               (vreset! polling? false)))
        (async/<! (async/timeout 100))
        (recur)))
    (->SpigotKafkaConsumer id @client topic-cfg polling?)))

(defn consumer#close [^Closeable client]
  (.close client))

(defn producer [{:keys [cfg topic-cfg]}]
  (->SpigotProducer (client*/producer cfg
                                      (.serializer (:key-serde topic-cfg))
                                      (.serializer (:val-serde topic-cfg)))
                    topic-cfg
                    true))

(defn producer#close [client]
  (u/silent! (ps/close! client)))

(defn topic-cfg [{:keys [name] :as cfg}]
  (merge cfg (sp.kcom/->topic-cfg name)))

(defn wf-topology [cfg]
  (-> (sp.kafka/default-builder)
      (sp.kafka/with-wf-topology cfg)
      .build))

(defn task-topology [cfg]
  (-> (sp.kafka/default-builder)
      (sp.kafka/with-task-topology cfg)
      .build))

(defn controller [{:keys [cfg opts topology]}]
  (let [ks (sp.kafka/start! topology cfg opts)]
    (->SpigotKafkaController ks)))

(defn controller#close [^Closeable controller]
  (.close controller))
