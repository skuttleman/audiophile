(ns audiophile.backend.infrastructure.workflows.core
  (:require
    [audiophile.backend.api.pubsub.core :as ps]
    [audiophile.backend.infrastructure.workflows.handlers :as wfh]
    [audiophile.common.core.utils.logger :as log]
    [audiophile.common.core.utils.maps :as maps]
    [kinsky.admin :as admin*]
    [spigot.context :as sp.ctx]
    [spigot.controllers.kafka.common :as sp.kcom]
    [spigot.controllers.kafka.core :as sp.kafka]
    [spigot.controllers.kafka.producer :as sp.kprod]
    [spigot.controllers.kafka.protocols :as sp.kproto])
  (:import
    (java.lang AutoCloseable)))

(defn ^:private extract-result [workflow-id data]
  (-> data
      :workflows/->result
      (sp.ctx/resolve-params (:ctx data))
      (assoc :workflow/id workflow-id
             :workflow/template (:workflows/template data))))

(deftype SpigotHandler [sys]
  sp.kproto/ISpigotTaskHandler
  (on-error [this ctx ex]
    (log/with-ctx [this :WF]
      (log/error ex "workflow failed" ctx)
      (ps/command-failed! (:events sys)
                          (:workflow/id ctx)
                          (maps/assoc-maybe ctx
                                            :error/reason (ex-message ex)
                                            :error/details (ex-data ex)))))
  (on-complete [this {workflow-id :workflow/id :as ctx} workflow]
    (log/with-ctx [this :WF]
      (log/info "workflow succeeded" ctx workflow)
      (ps/emit-event! (:events sys)
                      workflow-id
                      :workflow/completed
                      (extract-result workflow-id workflow)
                      ctx)))
  (process-task [this ctx task]
    (log/with-ctx [this :WF]
      (log/info "processing task" (select-keys task #{:spigot/id :spigot/tag}))
      (wfh/task-handler sys ctx task))))

(defn handler [sys]
  (->SpigotHandler sys))

(defn admin [{:keys [cfg]}]
  (admin*/client (log/spy cfg)))

(defn admin#close [^AutoCloseable client]
  (.close client))

(defn producer [{:keys [cfg topic-cfg]}]
  (sp.kprod/client cfg topic-cfg))

(defn producer#close [^AutoCloseable client]
  (.close client))

(defn topic-cfg [{:keys [name] :as cfg}]
  (merge cfg (sp.kcom/->topic-cfg name)))

(defn wf-controller [{:keys [cfg handler task-topic-cfg workflow-topic-cfg]}]
  (-> (sp.kafka/default-builder)
      (sp.kafka/build-topology handler task-topic-cfg workflow-topic-cfg)
      (sp.kafka/->streams cfg)
      (doto .start)))

(defn wf-controller#close [^AutoCloseable controller]
  (.close controller))

