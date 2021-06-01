(ns com.ben-allred.audiophile.api.app.repositories.teams.core
  (:refer-clojure :exclude [accessor])
  (:require
    [com.ben-allred.audiophile.api.app.interactors.protocols :as pint]
    [com.ben-allred.audiophile.api.app.repositories.core :as repos]
    [com.ben-allred.audiophile.api.app.repositories.teams.protocols :as pt]
    [com.ben-allred.audiophile.common.utils.logger :as log]))

(defn ^:private query-by-id* [executor team-id opts]
  (when-let [team (pt/find-by-team-id executor team-id opts)]
    (assoc team
           :team/members
           (pt/select-team-members executor team-id opts))))

(defn ^:private create* [executor opts]
  (let [team-id (pt/insert-team! executor opts opts)]
    (pt/find-by-team-id executor team-id nil)))

(deftype TeamAccessor [repo]
  pint/ITeamAccessor
  pint/IAccessor
  (query-many [_ opts]
    (repos/transact! repo pt/select-for-user (:user/id opts) opts))
  (query-one [_ opts]
    (repos/transact! repo query-by-id* (:team/id opts) opts))
  (create! [_ opts]
    (repos/transact! repo create* opts)))

(defn accessor [{:keys [repo]}]
  (->TeamAccessor repo))
