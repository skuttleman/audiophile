(ns audiophile.backend.infrastructure.templates.workflows
  (:require
    [audiophile.backend.api.pubsub.core :as ps]
    [audiophile.common.core.serdes.core :as serdes]
    [audiophile.common.core.serdes.impl :as serde]
    [audiophile.common.core.utils.logger :as log]
    [audiophile.common.core.utils.maps :as maps]
    [audiophile.common.core.utils.uuids :as uuids]
    [clojure.java.io :as io]
    [spigot.controllers.kafka.core :as sp.kafka]))

(defn load! [template]
  (let [filename (str "spigot/" (namespace template) "/" (name template) ".edn")]
    (serdes/deserialize serde/edn (io/input-stream (io/resource filename)))))

(defn setup [[tag & more :as form]]
  (let [[opts & children] (cond->> more
                            (not (map? (first more))) (cons {}))]
    (if (= tag :workflows/setup)
      (do (assert (= 1 (count children)) "workflows/setup supports exactly 1 child")
          [opts (first children)])
      [nil form])))

(defn workflow-spec [template ctx]
  (let [[setup form] (setup (load! template))
        [spec context] (maps/extract-keys setup #{:workflows/->result})]
    (assoc spec
           :workflows/template template
           :workflows/form form
           :workflows/ctx (maps/select-rename-keys ctx context))))

(defn start-workflow!
  ([producer template opts]
   (start-workflow! producer template {} opts))
  ([producer template ctx opts]
   (let [wf (workflow-spec template ctx)
         workflow-id (uuids/random)]
     (ps/send! producer {:key   workflow-id
                         :value (sp.kafka/create-wf-msg workflow-id wf (ps/->ctx opts))}))))

(defn generate-event [model-id event-type data {user-id :user/id :as ctx}]
  {:event/id         (uuids/random)
   :event/model-id   model-id
   :event/type       event-type
   :event/data       data
   :event/emitted-by user-id
   :event/ctx        (ps/->ctx ctx)})
