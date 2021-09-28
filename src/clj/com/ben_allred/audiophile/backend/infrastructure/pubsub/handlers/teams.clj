(ns com.ben-allred.audiophile.backend.infrastructure.pubsub.handlers.teams
  (:require
    [com.ben-allred.audiophile.backend.api.repositories.core :as repos]
    [com.ben-allred.audiophile.backend.api.repositories.teams.core :as rteams]
    [com.ben-allred.audiophile.backend.domain.interactors.protocols :as pint]
    [com.ben-allred.audiophile.backend.api.pubsub.core :as ps]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.common.core.utils.uuids :as uuids]))

(defn ^:private create* [executor team opts]
  (if (rteams/insert-team-access? executor team opts)
    (let [team-id (rteams/insert-team! executor team opts)]
      (rteams/find-event-team executor team-id))
    (throw (ex-info "insufficient access" {}))))

(deftype TeamCommandHandler [repo ch]
  pint/IMessageHandler
  (handle! [_ {command-id :command/id :command/keys [ctx data type] :as command}]
    (when (= type :team/create!)
      (try
        (log/info "saving team to db" command-id)
        (let [team (repos/transact! repo create* data ctx)]
          (ps/emit-event! ch (:team/id team) :team/created team ctx))
        (catch Throwable ex
          (log/error ex "failed: saving team to db" command)
          (try
            (ps/command-failed! ch
                                (or (:request/id ctx)
                                    (uuids/random))
                                (assoc ctx
                                       :error/command type
                                       :error/reason (.getMessage ex)))
            (catch Throwable ex
              (log/error ex "failed to emit command/failed"))))))))

(defn msg-handler [{:keys [ch repo]}]
  (->TeamCommandHandler repo ch))
