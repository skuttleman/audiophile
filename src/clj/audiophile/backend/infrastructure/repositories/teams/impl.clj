(ns audiophile.backend.infrastructure.repositories.teams.impl
  (:refer-clojure :exclude [accessor])
  (:require
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

(defn ^:private team-invite* [executor {:user/keys [email] :as data} opts]
  (when-not (qteams/invite-member-access? executor data opts)
    (int/no-access!))
  (merge opts data {:user/id (-> executor
                                 (qusers/find-by-email email opts)
                                 :user/id)}))

(deftype TeamAccessor [repo producer]
  pint/ITeamAccessor
  (team-invite! [_ data opts]
    (let [data (repos/transact! repo team-invite* data opts)]
      (crepos/start-workflow! producer :teams/invite data opts)))

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
    (crepos/start-workflow! producer :teams/update (merge opts data) opts)))

(defn accessor
  "Constructor for [[TeamAccessor]] which provides semantic access for storing and retrieving teams."
  [{:keys [producer repo]}]
  (->TeamAccessor repo producer))
