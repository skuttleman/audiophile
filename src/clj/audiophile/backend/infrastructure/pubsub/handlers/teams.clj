(ns audiophile.backend.infrastructure.pubsub.handlers.teams
  (:require
    [audiophile.backend.infrastructure.repositories.teams.queries :as q]
    [audiophile.backend.infrastructure.templates.workflows :as wf]
    [audiophile.common.core.utils.logger :as log]))

(defn ^:private create* [executor team opts]
  (if (q/insert-team-access? executor team opts)
    (q/insert-team! executor team opts)
    (throw (ex-info "insufficient access" {}))))

(wf/defhandler team/create!
  [executor _sys {command-id :command/id :command/keys [ctx data]}]
  (log/info "saving team to db" command-id)
  {:team/id (create* executor data ctx)})
