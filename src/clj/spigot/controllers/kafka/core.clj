(ns spigot.controllers.kafka.core
  (:require
    [spigot.controllers.kafka.common :as sp.kcom]
    [spigot.controllers.kafka.producer :as sp.kprod]
    [spigot.controllers.kafka.protocols :as sp.kproto]
    [spigot.controllers.kafka.topologies :as sp.ktop])
  (:import
    (java.util UUID)
    (org.apache.kafka.streams StreamsBuilder Topology KafkaStreams)))

(defn ^:private ->topic-cfg [topic]
  (cond-> topic
    (string? topic) sp.kcom/->topic-cfg))

(defn build-topology ^Topology
  [^StreamsBuilder builder {:keys [handler] :as opts}]
  {:pre [(satisfies? sp.kproto/ISpigotTaskHandler handler)]}
  (-> builder
      (doto (sp.ktop/task-processor-topology opts))
      (doto (sp.ktop/workflow-manager-topology opts))
      .build))

(defn start! [producer workflow ctx]
  (let [workflow-id (UUID/randomUUID)]
    @(sp.kprod/send! producer workflow-id [::sp.ktop/create! workflow (assoc ctx :workflow/id workflow-id)])
    workflow-id))

(defn default-builder []
  (StreamsBuilder.))

(defn ->streams [^Topology topology cfg]
  (KafkaStreams. topology (sp.kcom/->props cfg)))
