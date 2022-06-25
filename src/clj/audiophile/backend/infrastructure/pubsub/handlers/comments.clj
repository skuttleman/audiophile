(ns audiophile.backend.infrastructure.pubsub.handlers.comments
  (:require
    [audiophile.backend.api.pubsub.core :as ps]
    [audiophile.backend.infrastructure.pubsub.handlers.common :as hc]
    [audiophile.backend.infrastructure.repositories.comments.queries :as q]
    [audiophile.backend.infrastructure.templates.workflows :as wf]
    [audiophile.common.core.utils.logger :as log]))

(defn ^:private create* [executor comment opts]
  (if (q/insert-comment-access? executor comment opts)
    {:comment/id (q/insert-comment! executor comment opts)}
    (throw (ex-info "insufficient access" comment))))

(defmethod wf/command-handler :comment/create!
  [executor {:keys [commands events]} {command-id :command/id :command/keys [ctx data type]}]
  (log/with-ctx :CP
    (hc/with-command-failed! [events type ctx]
      (log/info "saving comment to db" command-id)
      (let [result {:spigot/id     (:spigot/id data)
                    :spigot/result (create* executor (:spigot/params data) ctx)}]
        (ps/emit-command! commands :workflow/next! result ctx)))))
