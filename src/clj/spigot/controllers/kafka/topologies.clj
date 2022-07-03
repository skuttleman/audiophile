(ns spigot.controllers.kafka.topologies
  (:require
    [spigot.controllers.kafka.protocols :as sp.kproto]
    [spigot.controllers.kafka.streams :as sp.ks]
    [spigot.core :as sp])
  (:import
    (org.apache.kafka.streams StreamsBuilder)))

(defn ^:private workflow-aggregator [agg [_ [tag params ctx]]]
  (case tag
    ::create! [(merge (sp/plan (:workflows/form params)
                               {:ctx (:workflows/ctx params)})
                      (dissoc params :workflows/form :workflows/ctx))
               ctx]
    ::next! [params ctx]
    ::result (update agg 0 sp/finish (:spigot/id params) (:spigot/result params))
    agg))

(defn ^:private workflow-flat-mapper [handler [k [wf ctx]]]
  (if (sp/finished? wf)
    (when-let [event (sp.kproto/on-complete handler ctx wf)]
      [[(:workflow/id ctx) [::event event]]])
    (let [[next-wf tasks] (sp/next-sync wf)]
      (when (or (seq tasks) (not= wf next-wf))
        (cons [k [::workflow [::next! next-wf ctx]]]
              (map (fn [{:spigot/keys [id] :as task}]
                     [id [::task {:task task :ctx ctx}]])
                   tasks))))))

(defn ^:private task-flat-mapper [handler [spigot-id {:keys [ctx task]}]]
  (let [[result ex] (try [(sp.kproto/process-task handler ctx task)]
                         (catch Throwable ex
                           [nil ex]))
        result {:spigot/id     spigot-id
                :spigot/result result}]
    (if ex
      (when-let [event (sp.kproto/on-error handler ctx ex)]
        [[(:workflow/id ctx) [::event event]]])
      [[(:workflow/id ctx) [::workflow [::result result]]]])))

(defn ->safe-handler [handler]
  (reify
    sp.kproto/ISpigotTaskHandler
    (on-complete [this ctx workflow]
      (try (sp.kproto/on-complete handler ctx workflow)
           (catch Throwable ex
             (sp.kproto/on-error this ctx ex))))
    (on-error [_ ctx ex]
      (try (sp.kproto/on-error handler ctx ex)
           (catch Throwable _
             nil)))
    (process-task [_ ctx task]
      (sp.kproto/process-task handler ctx task))))

(defn workflow-manager-topology
  [^StreamsBuilder builder {:keys [handler event-topic-cfg task-topic-cfg workflow-topic-cfg]}]
  (let [handler (->safe-handler handler)
        stream (-> builder
                   (sp.ks/stream workflow-topic-cfg)
                   (sp.ks/group-by-key workflow-topic-cfg)
                   (sp.ks/aggregate (constantly nil) workflow-aggregator workflow-topic-cfg)
                   .toStream
                   (sp.ks/flat-map (partial workflow-flat-mapper handler)))]
    (-> stream
        (sp.ks/filter (comp #{::workflow} first second))
        (sp.ks/map-values second)
        (sp.ks/to workflow-topic-cfg))
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
  (let [handler (->safe-handler handler)
        stream (-> builder
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
