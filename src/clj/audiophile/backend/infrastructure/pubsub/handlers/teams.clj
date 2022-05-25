(ns audiophile.backend.infrastructure.pubsub.handlers.teams
  (:require
    [audiophile.backend.api.pubsub.core :as ps]
    [audiophile.backend.api.repositories.core :as repos]
    [audiophile.backend.api.repositories.teams.core :as rteams]
    [audiophile.backend.domain.interactors.protocols :as pint]
    [audiophile.backend.infrastructure.pubsub.handlers.common :as hc]
    [audiophile.common.core.utils.logger :as log]))

(defn ^:private create* [executor team opts]
  (if (rteams/insert-team-access? executor team opts)
    (let [team-id (rteams/insert-team! executor team opts)]
      (rteams/find-event-team executor team-id))
    (throw (ex-info "insufficient access" {}))))

(deftype TeamCommandHandler [repo ch]
  pint/IMessageHandler
  (handle? [_ msg]
    (= :team/create! (:command/type msg)))
  (handle! [this {command-id :command/id :command/keys [ctx data type]}]
    (log/with-ctx [this :CP]
      (hc/with-command-failed! [ch type ctx]
        (log/info "saving team to db" command-id)
        (let [team (repos/transact! repo create* data ctx)]
          (ps/emit-event! ch (:team/id team) :team/created team ctx))))))

(defn msg-handler [{:keys [ch repo]}]
  (->TeamCommandHandler repo ch))
