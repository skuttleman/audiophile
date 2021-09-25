(ns com.ben-allred.audiophile.backend.infrastructure.pubsub.handlers.teams
  (:require
    [com.ben-allred.audiophile.backend.api.repositories.core :as repos]
    [com.ben-allred.audiophile.backend.api.repositories.teams.core :as rteams]
    [com.ben-allred.audiophile.backend.domain.interactors.protocols :as pint]
    [com.ben-allred.audiophile.backend.infrastructure.pubsub.core :as ps]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]))

(defn ^:private create* [executor team opts]
  (if (rteams/insert-team-access? executor team opts)
    (let [team-id (rteams/insert-team! executor team opts)]
      (rteams/find-event-team executor team-id))
    (throw (ex-info "insufficient access" {}))))

(deftype TeamCommandHandler [repo pubsub]
  pint/IMessageHandler
  (handle! [_ {[command-id {:command/keys [type] :as command} ctx] :msg :as msg}]
    (when (= type :team/create!)
      (try
        (log/info "saving team to db" command-id)
        (let [team (repos/transact! repo create* (:command/data command) ctx)]
          (ps/emit-event! pubsub (:user/id ctx) (:team/id team) :team/created team ctx))
        (catch Throwable ex
          (log/error ex "failed: saving team to db" msg)
          (try
            (ps/command-failed! pubsub
                                (:request/id ctx)
                                (assoc ctx
                                       :error/command (:command/type command)
                                       :error/reason (.getMessage ex)))
            (catch Throwable ex
              (log/error ex "failed to emit command/failed"))))))))

(defn msg-handler [{:keys [repo pubsub]}]
  (->TeamCommandHandler repo pubsub))
