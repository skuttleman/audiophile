(ns audiophile.backend.infrastructure.pubsub.handlers.projects
  (:require
    [audiophile.backend.api.pubsub.core :as ps]
    [audiophile.backend.infrastructure.pubsub.handlers.common :as hc]
    [audiophile.backend.infrastructure.repositories.projects.queries :as q]
    [audiophile.backend.infrastructure.templates.workflows :as wf]
    [audiophile.common.core.utils.logger :as log]))

(defn ^:private create* [executor project opts]
  (if (q/insert-project-access? executor project opts)
    {:project/id (q/insert-project! executor project opts)}
    (throw (ex-info "insufficient access" project))))

(defmethod wf/command-handler :project/create!
  [executor {:keys [commands events]} {command-id :command/id :command/keys [ctx data type]}]
  (log/with-ctx :CP
    (hc/with-command-failed! [events type ctx]
      (log/info "saving project to db" command-id)
      (let [result {:spigot/id     (:spigot/id data)
                    :spigot/result (create* executor (:spigot/params data) ctx)}]
        (ps/emit-command! commands :workflow/next! result ctx)))))
