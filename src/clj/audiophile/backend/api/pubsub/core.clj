(ns audiophile.backend.api.pubsub.core
  (:require
    [audiophile.backend.api.pubsub.protocols :as pps]
    [audiophile.backend.infrastructure.templates.workflows :as wf]
    [audiophile.common.api.pubsub.core :as pubsub]
    [audiophile.common.core.utils.logger :as log]
    [audiophile.common.core.utils.maps :as maps]
    [audiophile.common.core.utils.uuids :as uuids]
    [spigot.controllers.kafka.core :as sp.kafka]))

(defn open? [ch]
  (pps/open? ch))

(defn send! [ch msg]
  (pps/send! ch msg)
  nil)

(defn close! [ch]
  (pps/close! ch)
  nil)

(defn ^:private ->ctx [ctx]
  (into {} (filter (comp #{"id"} name key)) ctx))

(defn workflow-spec [template ctx]
  (let [[setup form] (wf/setup (wf/load! template))
        [spec context] (maps/extract-keys setup #{:workflows/->result})]
    (assoc spec
           :workflows/template template
           :workflows/form form
           :workflows/ctx (maps/select-rename-keys ctx context))))

(defn broadcast!
  "Broadcast an event to all connections"
  ([pubsub event-id event]
   (broadcast! pubsub event-id event nil))
  ([pubsub event-id event ctx]
   (pubsub/publish! pubsub [::broadcast] [event-id event (->ctx ctx)])))

(defn send-user!
  "Broadcast an event to all connections for a specific user"
  ([pubsub user-id event-id event]
   (send-user! pubsub user-id event-id event nil))
  ([pubsub user-id msg-id msg ctx]
   (pubsub/publish! pubsub [::user user-id] [msg-id msg (assoc (->ctx ctx) :user/id user-id)])))

(defn generate-event [model-id event-type data {user-id :user/id :as ctx}]
  {:event/id         (uuids/random)
   :event/model-id   model-id
   :event/type       event-type
   :event/data       data
   :event/emitted-by user-id
   :event/ctx        (->ctx ctx)})

(defn emit-event! [ch model-id event-type data ctx]
  (let [event (generate-event model-id event-type data ctx)]
    (send! ch event)
    (:event/id event)))

(defn emit-command! [ch command-type data {user-id :user/id :as ctx}]
  (let [command-id (uuids/random)
        command {:command/id         command-id
                 :command/type       command-type
                 :command/data       data
                 :command/emitted-by user-id
                 :command/ctx        (->ctx ctx)}]
    (send! ch command)
    command-id))

(defn start-workflow!
  ([producer template opts]
   (start-workflow! producer template {} opts))
  ([producer template ctx opts]
   (let [wf (workflow-spec template ctx)
         workflow-id (uuids/random)]
     (send! producer {:key   workflow-id
                      :value (sp.kafka/create-wf-msg workflow-id wf (->ctx opts))}))))

(defn command-failed! [ch model-id opts]
  (let [[data ctx] (maps/extract-keys opts #{:error/command :error/reason :error/details})]
    (emit-event! ch model-id :command/failed data ctx)))
