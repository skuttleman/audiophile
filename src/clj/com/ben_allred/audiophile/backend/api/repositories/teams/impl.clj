(ns com.ben-allred.audiophile.backend.api.repositories.teams.impl
  (:refer-clojure :exclude [accessor])
  (:require
    [com.ben-allred.audiophile.backend.api.repositories.core :as repos]
    [com.ben-allred.audiophile.backend.api.repositories.teams.core :as rteams]
    [com.ben-allred.audiophile.backend.domain.interactors.protocols :as pint]
    [com.ben-allred.audiophile.backend.infrastructure.pubsub.core :as ps]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]))

(defn ^:private query-by-id* [executor team-id opts]
  (when-let [team (rteams/find-by-team-id executor team-id opts)]
    (assoc team
           :team/members
           (rteams/select-team-members executor team-id opts))))

(defn ^:private create* [executor team opts]
  (if (rteams/insert-team-access? executor team opts)
    (let [team-id (rteams/insert-team! executor team opts)]
      (rteams/find-event-team executor team-id))
    (throw (ex-info "insufficient access" {}))))

(deftype TeamAccessor [repo commands events]
  pint/ITeamAccessor
  pint/IAccessor
  (query-many [_ opts]
    (repos/transact! repo rteams/select-for-user (:user/id opts) opts))
  (query-one [_ opts]
    (repos/transact! repo query-by-id* (:team/id opts) opts))
  (create! [_ data opts]
    (ps/emit-command! commands (:user/id opts) :team/create! data opts))

  pint/IMessageHandler
  (handle! [_ {[command-id {:command/keys [type] :as command} ctx] :msg :as msg}]
    (when (= type :team/create!)
      (try
        (log/info "saving team to db" command-id)
        (let [team (repos/transact! repo create* (:command/data command) ctx)]
          (ps/emit-event! events (:user/id ctx) (:team/id team) :team/created team ctx))
        (catch Throwable ex
          (log/error ex "failed: saving team to db" msg)
          (try
            (ps/command-failed! events
                                (:request/id ctx)
                                (assoc ctx
                                       :error/command (:command/type command)
                                       :error/reason (.getMessage ex)))
            (catch Throwable ex
              (log/error ex "failed to emit command/failed"))))))))

(defn accessor
  "Constructor for [[TeamAccessor]] which provides semantic access for storing and retrieving teams."
  [{:keys [commands events repo]}]
  (->TeamAccessor repo commands events))
