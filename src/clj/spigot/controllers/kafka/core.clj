(ns spigot.controllers.kafka.core
  (:require
    [spigot.controllers.kafka.common :as sp.kcom]
    [spigot.controllers.kafka.topologies :as sp.ktop]
    [spigot.controllers.protocols :as sp.pcon]
    [taoensso.timbre :as log])
  (:import
    (org.apache.kafka.streams KafkaStreams KafkaStreams$State
                              KafkaStreams$StateListener
                              StreamsBuilder Topology)))

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

(defn running? [^KafkaStreams kafka-streams]
  (contains? #{KafkaStreams$State/CREATED
               KafkaStreams$State/REBALANCING
               KafkaStreams$State/RUNNING}
             (.state kafka-streams)))

(defn start! ^KafkaStreams [streams-cfg builder-opts]
  (let [timeout (:timeout builder-opts 30000)
        started (promise)
        stopped (promise)
        ks (-> (default-builder)
               (build-topology builder-opts)
               (->streams streams-cfg)
               (doto
                 (.setStateListener (reify KafkaStreams$StateListener
                                      (onChange [_ curr old]
                                        (log/debugf "KafkaStreams state transition from [%s] to [%s]" old curr)
                                        (when (= KafkaStreams$State/RUNNING curr)
                                          (deliver started true))
                                        (when (and (realized? started)
                                                   (contains? #{KafkaStreams$State/NOT_RUNNING
                                                                KafkaStreams$State/ERROR}
                                                              curr))
                                          (deliver stopped true)))))
                 .start))]
    (when-not (deref started timeout false)
      (throw (ex-info "KafkaStreams did not start within timeout" {:timeout timeout})))
    {:streams ks
     :stopped stopped
     :timeout timeout}))

(defn stop! [{:keys [stopped ^KafkaStreams streams timeout]}]
  (.close streams)
  (when-not (deref stopped timeout false)
    (throw (ex-info "KafkaStreams did not stop within timeout" {:timeout timeout}))))
