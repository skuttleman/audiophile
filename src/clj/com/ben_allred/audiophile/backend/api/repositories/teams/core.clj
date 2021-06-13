(ns com.ben-allred.audiophile.backend.api.repositories.teams.core
  (:refer-clojure :exclude [accessor])
  (:require
    [com.ben-allred.audiophile.backend.api.repositories.common :as crepos]
    [com.ben-allred.audiophile.backend.api.repositories.core :as repos]
    [com.ben-allred.audiophile.backend.api.repositories.teams.protocols :as pt]
    [com.ben-allred.audiophile.backend.domain.interactors.protocols :as pint]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]))

(defn ^:private query-by-id* [executor team-id opts]
  (when-let [team (pt/find-by-team-id executor team-id opts)]
    (assoc team
           :team/members
           (pt/select-team-members executor team-id opts))))

(defn ^:private create* [executor opts]
  (let [team-id (pt/insert-team! executor opts opts)
        team (pt/find-event-team executor team-id)]
    (pt/team-created! executor (:user/id opts) team opts)))

(deftype TeamAccessor [repo]
  pint/ITeamAccessor
  pint/IAccessor
  (query-many [_ opts]
    (repos/transact! repo pt/select-for-user (:user/id opts) opts))
  (query-one [_ opts]
    (repos/transact! repo query-by-id* (:team/id opts) opts))
  (create! [_ opts]
    (crepos/command! repo opts
      (repos/transact! repo create* opts))))

(defn accessor
  "Constructor for [[TeamAccessor]] which provides semantic access for storing and retrieving teams."
  [{:keys [repo]}]
  (->TeamAccessor repo))
