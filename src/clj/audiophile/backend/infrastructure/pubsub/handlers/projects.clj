(ns audiophile.backend.infrastructure.pubsub.handlers.projects
  (:require
    [audiophile.backend.api.pubsub.core :as ps]
    [audiophile.backend.api.repositories.core :as repos]
    [audiophile.backend.api.repositories.projects.queries :as q]
    [audiophile.backend.domain.interactors.protocols :as pint]
    [audiophile.backend.infrastructure.pubsub.handlers.common :as hc]
    [audiophile.common.core.utils.logger :as log]))

(defn ^:private create* [executor project opts]
  (if (q/insert-project-access? executor project opts)
    (let [project-id (q/insert-project! executor project opts)]
      (q/find-event-project executor project-id))
    (throw (ex-info "insufficient access" project))))

(defn ^:private project-command-handler#handle!
  [this repo ch {command-id :command/id :command/keys [ctx data type]}]
  (log/with-ctx [this :CP]
    (hc/with-command-failed! [ch type ctx]
      (log/info "saving project to db" command-id)
      (let [project (repos/transact! repo create* data ctx)]
        (ps/emit-event! ch (:project/id project) :project/created project ctx)))))

(deftype ProjectCommandHandler [repo ch]
  pint/IMessageHandler
  (handle? [_ msg]
    (= :project/create! (:command/type msg)))
  (handle! [this msg]
    (project-command-handler#handle! this repo ch msg)))

(defn msg-handler [{:keys [ch repo]}]
  (->ProjectCommandHandler repo ch))
