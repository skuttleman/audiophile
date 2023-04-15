(ns spigot.controllers.kafka.topologies
  (:require
    [audiophile.common.core.utils.core :as u]
    [audiophile.common.core.utils.logger :as log]
    [spigot.controllers.protocols :as sp.pcon]
    [spigot.controllers.kafka.streams :as sp.ks]
    [spigot.core :as sp]
    [spigot.impl.api :as spapi])
  (:import
    (org.apache.kafka.streams StreamsBuilder)))

(defmulti ^:private workflow-aggregator
          (fn [_ [_ [tag]]]
            tag))

(defmethod workflow-aggregator ::create!
  [_ [_ [_ params ctx]]]
  (let [init (-> (:workflows/form params)
                 (sp/create (:workflows/ctx params))
                 (merge (dissoc params :workflows/form :workflows/ctx)))
        [wf tasks] (sp/next init)]
    {:wf    wf
     :ctx   ctx
     :tasks tasks
     :init  init}))

(defmethod workflow-aggregator ::result
  [{:keys [ctx wf]} [_ [_ {:spigot/keys [id result status]}]]]
  (let [[wf' tasks] (-> wf
                        (cond-> (= ::success status) (sp/succeed! id result)
                                (= ::failure status) (sp/fail! id result))
                        sp/next)]
    {:wf    wf'
     :ctx   ctx
     :tasks tasks}))

(defn ^:private workflow-flat-mapper [handler [workflow-id {:keys [wf ctx tasks init]}]]
  (log/debug "processing workflow" ctx (spapi/scope wf))
  (cond
    (= :success (sp/status wf))
    (when-let [complete-event (u/silent! (sp.pcon/on-complete handler ctx wf))]
      [[workflow-id [::event complete-event]]])

    (= :failure (sp/status wf))
    (when-let [err-event (u/silent! (sp.pcon/on-error handler ctx wf))]
      [[workflow-id [::event err-event]]])

    :else
    (let [event (if init
                  (u/silent! (sp.pcon/on-create handler ctx init))
                  (u/silent! (sp.pcon/on-update handler ctx wf)))]
      (cond->> (map (fn [[_ {:spigot/keys [id]} :as task]]
                      [id [::task {:ctx         ctx
                                   :task        task
                                   :workflow-id workflow-id}]])
                    tasks)
        event (cons [workflow-id [::event event]])))))

(defn ^:private task-flat-mapper [handler [spigot-id {:keys [ctx task workflow-id]}]]
  (let [[result ex] (try [(sp.pcon/process-task handler ctx task)]
                         (catch Throwable ex
                           (log/error ex "error while processing workflow task")
                           [nil ex]))
        result {:spigot/id     spigot-id
                :spigot/status (if ex ::failure ::success)
                :spigot/result (if ex (ex-data ex) result)}]
    [[workflow-id [::workflow [::result result]]]]))

(defn workflow-manager-topology
  [^StreamsBuilder builder {:keys [handler event-topic-cfg task-topic-cfg workflow-topic-cfg]}]
  (let [stream (-> builder
                   (sp.ks/stream workflow-topic-cfg)
                   (sp.ks/group-by-key workflow-topic-cfg)
                   (sp.ks/aggregate (constantly nil) workflow-aggregator workflow-topic-cfg)
                   .toStream
                   (sp.ks/flat-map (partial workflow-flat-mapper handler)))]
    (-> stream
        (sp.ks/filter (comp #{::task} first second))
        (sp.ks/map-values second)
        (sp.ks/to task-topic-cfg))
    (when event-topic-cfg
      (-> stream
          (sp.ks/filter (comp #{::event} first second))
          (sp.ks/map-values second)
          (sp.ks/to event-topic-cfg)))))

(defn task-processor-topology
  [^StreamsBuilder builder {:keys [handler event-topic-cfg task-topic-cfg workflow-topic-cfg]}]
  (let [stream (-> builder
                   (sp.ks/stream task-topic-cfg)
                   (sp.ks/flat-map (partial task-flat-mapper handler)))]
    (-> stream
        (sp.ks/filter (comp #{::workflow} first second))
        (sp.ks/map-values second)
        (sp.ks/to workflow-topic-cfg))
    (when event-topic-cfg
      (-> stream
          (sp.ks/filter (comp #{::event} first second))
          (sp.ks/map-values second)
          (sp.ks/to event-topic-cfg)))))
