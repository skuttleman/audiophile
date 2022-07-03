(ns audiophile.backend.infrastructure.pubsub.handlers.projects
  (:require
    [audiophile.backend.infrastructure.repositories.projects.queries :as qprojects]
    [audiophile.backend.infrastructure.templates.workflows :as wf]
    [audiophile.common.core.utils.logger :as log]))

(defn ^:private create* [executor project opts]
  (if (qprojects/insert-project-access? executor project opts)
    (qprojects/insert-project! executor project opts)
    (throw (ex-info "insufficient access" project))))

(wf/defhandler project/create!
  [executor _sys {command-id :command/id :command/keys [ctx data]}]
  (log/info "saving project to db" command-id)
  {:project/id (create* executor data ctx)})
