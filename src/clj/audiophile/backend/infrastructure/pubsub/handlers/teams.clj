(ns audiophile.backend.infrastructure.pubsub.handlers.teams
  (:require
    [audiophile.backend.api.pubsub.core :as ps]
    [audiophile.backend.infrastructure.pubsub.handlers.common :as hc]
    [audiophile.backend.infrastructure.repositories.teams.queries :as q]
    [audiophile.backend.infrastructure.templates.workflows :as wf]
    [audiophile.common.core.utils.logger :as log]))

(defn ^:private create* [executor team opts]
  (if (q/insert-team-access? executor team opts)
    (let [team-id (q/insert-team! executor team opts)]
      (q/find-event-team executor team-id))
    (throw (ex-info "insufficient access" {}))))

(defmethod wf/command-handler :team/create!
  [executor {:keys [commands events]} {command-id :command/id :command/keys [ctx data type]}]
  (log/with-ctx :CP
    (hc/with-command-failed! [events type ctx]
      (log/info "saving team to db" command-id)
      (let [result {:spigot/id     (:spigot/id data)
                    :spigot/result (create* executor (:spigot/params data) (:spigot/params data))}]
        (ps/emit-command! commands :workflow/next! result ctx)))))
