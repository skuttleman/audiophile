(ns spigot.controllers.kafka.core
  (:require
    [spigot.controllers.kafka.common :as sp.kcom]
    [spigot.controllers.kafka.topologies :as sp.ktop]
    [spigot.controllers.protocols :as sp.pcon])
  (:import
    (org.apache.kafka.streams KafkaStreams StreamsBuilder Topology)))

(defn ^:private ->topic-cfg [topic]
  (cond-> topic
    (string? topic) sp.kcom/->topic-cfg))

(defn create-wf-msg [workflow-id workflow ctx]
  [::sp.ktop/create! workflow (assoc ctx :workflow/id workflow-id)])

(defn build-topology ^Topology
  [^StreamsBuilder builder {:keys [handler] :as opts}]
  {:pre [(satisfies? sp.pcon/ISpigotTaskHandler handler)]}
  (-> builder
      (doto (sp.ktop/task-processor-topology opts))
      (doto (sp.ktop/workflow-manager-topology opts))
      .build))

(defn default-builder ^StreamsBuilder []
  (StreamsBuilder.))

(defn ->streams ^KafkaStreams [^Topology topology cfg]
  (KafkaStreams. topology (sp.kcom/->props cfg)))

(defn start! ^KafkaStreams [streams-cfg builder-opts]
  (-> (default-builder)
      (build-topology builder-opts)
      (->streams streams-cfg)
      (doto .start)))