(ns com.ben-allred.audiophile.api.services.repositories.teams.core
  (:refer-clojure :exclude [accessor])
  (:require
    [com.ben-allred.audiophile.api.services.interactors.protocols :as pint]
    [com.ben-allred.audiophile.api.services.repositories.core :as repos]
    [com.ben-allred.audiophile.api.services.repositories.teams.queries :as q]
    [com.ben-allred.audiophile.common.utils.logger :as log]))

(defn ^:private query-by-id* [executor team-id opts]
  (when-let [team (q/find-by-team-id executor team-id opts)]
    (assoc team
           :team/members
           (q/select-team-members executor team-id))))

(defn ^:private create* [executor opts]
  (let [team-id (q/insert-team! executor opts opts)]
    (q/find-by-team-id executor team-id)))

(deftype TeamAccessor [repo]
  pint/ITeamAccessor
  pint/IAccessor
  (query-many [_ opts]
    (repos/transact! repo q/select-for-user (:user/id opts)))
  (query-one [_ opts]
    (repos/transact! repo query-by-id* (:team/id opts) opts))
  (create! [_ opts]
    (repos/transact! repo create* opts)))

(defn accessor [{:keys [repo]}]
  (->TeamAccessor repo))
