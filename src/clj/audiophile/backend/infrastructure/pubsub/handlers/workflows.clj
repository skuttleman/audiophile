(ns audiophile.backend.infrastructure.pubsub.handlers.workflows
  (:require
    [audiophile.backend.api.pubsub.core :as ps]
    [audiophile.backend.domain.interactors.protocols :as pint]
    [audiophile.backend.infrastructure.pubsub.handlers.common :as hc]
    [audiophile.backend.infrastructure.repositories.core :as repos]
    [audiophile.backend.infrastructure.repositories.workflows.queries :as q]
    [audiophile.backend.infrastructure.templates.workflows :as wf]
    [audiophile.common.core.utils.logger :as log]
    [audiophile.common.core.utils.maps :as maps]
    [spigot.context :as sp.ctx]
    [spigot.core :as sp]))

(defn ^:private extract-result [workflow-id data]
  (-> data
      :workflows/->result
      (sp.ctx/resolve-params (:ctx data))
      (assoc :workflow/id workflow-id)))

(defn ^:private next* [executor workflow-id data ctx {:keys [commands events]}]
  (if (sp/finished? data)
    (do (q/update-by-id! executor workflow-id {:status "completed"})
        (ps/emit-event! events
                        workflow-id
                        :workflow/completed
                        (extract-result workflow-id data)
                        ctx))
    (let [data (sp/next data
                        (fn [task]
                          (ps/emit-command! commands
                                            (:spigot/tag task)
                                            (select-keys task #{:spigot/id :spigot/params})
                                            (assoc ctx :workflow/id workflow-id))))]
      (q/update-by-id! executor workflow-id {:status "running" :data data}))))

(defmethod wf/command-handler :workflow/create!
  [executor sys {:command/keys [ctx data]}]
  (let [workflow-id (q/create! executor data)
        workflow (q/select-for-update executor workflow-id)]
    (next* executor workflow-id (:workflow/data workflow) ctx sys)))

(defmethod wf/command-handler :workflow/next!
  [executor sys {:command/keys [ctx data]}]
  (let [workflow-id (:workflow/id ctx)
        workflow (q/select-for-update executor workflow-id)
        data (cond-> (:workflow/data workflow)
               (:spigot/id data) (sp/finish (:spigot/id data) (:spigot/result data)))]
    (next* executor workflow-id data ctx sys)))

(deftype WorkflowHandler [repo sys]
  pint/IMessageHandler
  (handle? [_ msg]
    (contains? (methods wf/command-handler) (:command/type msg)))
  (handle! [_ {:command/keys [ctx type] :as msg}]
    (hc/with-command-failed! [(:events sys) type ctx]
      (repos/transact! repo wf/command-handler sys msg))))

(defn msg-handler [{:keys [commands events jwt-serde repo]}]
  (->WorkflowHandler repo (maps/->m commands events jwt-serde)))
