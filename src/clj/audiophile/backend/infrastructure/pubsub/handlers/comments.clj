(ns audiophile.backend.infrastructure.pubsub.handlers.comments
  (:require
    [audiophile.backend.infrastructure.repositories.comments.queries :as q]
    [audiophile.backend.infrastructure.templates.workflows :as wf]
    [audiophile.common.core.utils.logger :as log]))

(defn ^:private create* [executor comment opts]
  (if (q/insert-comment-access? executor comment opts)
    (q/insert-comment! executor comment opts)
    (throw (ex-info "insufficient access" comment))))

(wf/defhandler comment/create! [executor _sys {command-id :command/id :command/keys [ctx data]}]
  (log/info "saving comment to db" command-id)
  {:comment/id (create* executor data ctx)})
