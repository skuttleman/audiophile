(ns audiophile.backend.api.repositories.teams.impl
  (:refer-clojure :exclude [accessor])
  (:require
    [audiophile.backend.api.repositories.core :as repos]
    [audiophile.backend.api.repositories.teams.core :as rteams]
    [audiophile.backend.domain.interactors.protocols :as pint]
    [audiophile.backend.api.pubsub.core :as ps]
    [audiophile.common.core.utils.logger :as log]))

(defn ^:private query-by-id* [executor team-id opts]
  (when-let [team (rteams/find-by-team-id executor team-id opts)]
    (assoc team
           :team/members
           (rteams/select-team-members executor team-id opts))))

(deftype TeamAccessor [repo ch]
  pint/ITeamAccessor
  pint/IAccessor
  (query-many [_ opts]
    (repos/transact! repo rteams/select-for-user (:user/id opts) opts))
  (query-one [_ opts]
    (repos/transact! repo query-by-id* (:team/id opts) opts))
  (create! [_ data opts]
    (ps/emit-command! ch :team/create! data opts)))

(defn accessor
  "Constructor for [[TeamAccessor]] which provides semantic access for storing and retrieving teams."
  [{:keys [ch repo]}]
  (->TeamAccessor repo ch))
