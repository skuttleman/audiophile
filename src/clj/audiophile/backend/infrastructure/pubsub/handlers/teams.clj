(ns audiophile.backend.infrastructure.pubsub.handlers.teams
  (:require
    [audiophile.backend.api.pubsub.core :as ps]
    [audiophile.backend.api.repositories.core :as repos]
    [audiophile.backend.api.repositories.teams.queries :as q]
    [audiophile.backend.domain.interactors.protocols :as pint]
    [audiophile.backend.infrastructure.pubsub.handlers.common :as hc]
    [audiophile.common.core.utils.logger :as log]))

(defn ^:private create* [executor team opts]
  (if (q/insert-team-access? executor team opts)
    (let [team-id (q/insert-team! executor team opts)]
      (q/find-event-team executor team-id))
    (throw (ex-info "insufficient access" {}))))

(defn ^:private team-command-handler#handle! [this repo ch {command-id :command/id :command/keys [ctx data type]}]
  (log/with-ctx [this :CP]
    (hc/with-command-failed! [ch type ctx]
      (log/info "saving team to db" command-id)
      (let [team (repos/transact! repo create* data ctx)]
        (ps/emit-event! ch (:team/id team) :team/created team ctx)))))

(deftype TeamCommandHandler [repo ch]
  pint/IMessageHandler
  (handle? [_ msg]
    (= :team/create! (:command/type msg)))
  (handle! [this msg]
    (team-command-handler#handle! this repo ch msg)))

(defn msg-handler [{:keys [ch repo]}]
  (->TeamCommandHandler repo ch))
