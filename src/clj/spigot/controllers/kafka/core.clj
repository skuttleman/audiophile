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

(defn ^:private ->streams ^KafkaStreams [^Topology topology cfg]
  (KafkaStreams. topology (sp.kcom/->props cfg)))

(defn create-wf-msg [workflow-id workflow ctx]
  [::sp.ktop/create! workflow (assoc ctx :workflow/id workflow-id)])

(defn default-builder ^StreamsBuilder []
  (StreamsBuilder.))

(defn running? [^KafkaStreams kafka-streams]
  (contains? #{KafkaStreams$State/CREATED
               KafkaStreams$State/REBALANCING
               KafkaStreams$State/RUNNING}
             (.state kafka-streams)))

(defn with-wf-topology ^StreamsBuilder [^StreamsBuilder builder opts]
  (doto builder
    (sp.ktop/workflow-manager-topology opts)))

(defn with-task-topology ^StreamsBuilder [^StreamsBuilder builder opts]
  (doto builder
    (sp.ktop/task-processor-topology opts)))

(defn start! ^KafkaStreams [^Topology topology streams-cfg opts]
  (let [timeout (:timeout opts 30000)
        started (promise)
        stopped (promise)
        streams (-> topology
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
    {:streams streams
     :stopped stopped
     :timeout timeout}))

(defn stop! [{:keys [stopped ^KafkaStreams streams timeout]}]
  (.close streams)
  (when-not (deref stopped timeout false)
    (throw (ex-info "KafkaStreams did not stop within timeout" {:timeout timeout}))))
