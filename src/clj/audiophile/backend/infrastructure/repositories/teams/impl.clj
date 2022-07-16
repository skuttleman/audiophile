(ns audiophile.backend.infrastructure.repositories.teams.impl
  (:refer-clojure :exclude [accessor])
  (:require
    [audiophile.backend.api.pubsub.core :as ps]
    [audiophile.backend.domain.interactors.core :as int]
    [audiophile.backend.domain.interactors.protocols :as pint]
    [audiophile.backend.infrastructure.repositories.common :as crepos]
    [audiophile.backend.infrastructure.repositories.core :as repos]
    [audiophile.backend.infrastructure.repositories.projects.queries :as qprojects]
    [audiophile.backend.infrastructure.repositories.teams.queries :as qteams]
    [audiophile.backend.infrastructure.repositories.users.queries :as qusers]
    [audiophile.common.core.utils.logger :as log]))

(defn ^:private query-by-id* [executor team-id opts]
  (when-let [team (qteams/find-by-team-id executor team-id opts)]
    (assoc team
           :team/members
           (qteams/select-team-members executor team-id opts)
           :team/projects
           (qprojects/select-for-team executor team-id opts))))

(deftype TeamAccessor [repo producer]
  pint/ITeamAccessor
  (team-invite! [_ data opts]
    (when-not (repos/transact! repo qteams/invite-member-access? data opts)
      (int/no-access!))
    (let [user-id (:user/id (repos/transact! repo qusers/find-by-email (:user/email data) opts))
          opts (cond-> (assoc opts :subscription/topics #{[:teams (:team/id data)]})
                 user-id (update :subscription/topics conj [::ps/user user-id]))]
      (crepos/start-workflow! producer :teams/invite (merge opts data) opts)))

  pint/IAccessor
  (query-many [_ opts]
    (repos/transact! repo qteams/select-for-user (:user/id opts) opts))
  (query-one [_ opts]
    (repos/transact! repo query-by-id* (:team/id opts) opts))
  (create! [_ data opts]
    (when-not (repos/transact! repo qteams/insert-team-access? data opts)
      (int/no-access!))
    (crepos/start-workflow! producer :teams/create (merge opts data) opts))
  (update! [_ data opts]
    (when-not (repos/transact! repo qteams/update-team-access? data opts)
      (int/no-access!))
    (let [opts (assoc opts :subscription/topics #{[:teams (:team/id data)]})]
      (crepos/start-workflow! producer :teams/update (merge opts data) opts))))

(defn accessor
  "Constructor for [[TeamAccessor]] which provides semantic access for storing and retrieving teams."
  [{:keys [producer repo]}]
  (->TeamAccessor repo producer))
