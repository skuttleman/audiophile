(ns com.ben-allred.audiophile.backend.api.repositories.teams.impl
  (:refer-clojure :exclude [accessor])
  (:require
    [com.ben-allred.audiophile.backend.api.repositories.common :as crepos]
    [com.ben-allred.audiophile.backend.api.repositories.core :as repos]
    [com.ben-allred.audiophile.backend.api.repositories.teams.core :as rteams]
    [com.ben-allred.audiophile.backend.domain.interactors.protocols :as pint]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]))

(defn ^:private query-by-id* [executor team-id opts]
  (when-let [team (rteams/find-by-team-id executor team-id opts)]
    (assoc team
           :team/members
           (rteams/select-team-members executor team-id opts))))

(defn ^:private create* [executor data opts]
  (let [team-id (rteams/insert-team! executor data opts)
        team (rteams/find-event-team executor team-id)]
    (rteams/team-created! executor (:user/id opts) team opts)))

(deftype TeamAccessor [repo]
  pint/ITeamAccessor
  pint/IAccessor
  (query-many [_ opts]
    (repos/transact! repo rteams/select-for-user (:user/id opts) opts))
  (query-one [_ opts]
    (repos/transact! repo query-by-id* (:team/id opts) opts))
  (create! [_ data opts]
    (crepos/command! repo opts
      (repos/transact! repo create* data opts))))

(defn accessor
  "Constructor for [[TeamAccessor]] which provides semantic access for storing and retrieving teams."
  [{:keys [repo]}]
  (->TeamAccessor repo))
