(ns com.ben-allred.audiophile.api.services.repositories.teams.core
  (:require
    [com.ben-allred.audiophile.api.services.interactors.protocols :as pint]
    [com.ben-allred.audiophile.api.services.repositories.common :as crepos]
    [com.ben-allred.audiophile.api.services.repositories.core :as repos]
    [com.ben-allred.audiophile.api.services.repositories.teams.queries :as q]
    [com.ben-allred.audiophile.api.services.repositories.users.queries :as qu]
    [com.ben-allred.audiophile.common.utils.colls :as colls]
    [com.ben-allred.audiophile.common.utils.logger :as log]
    [integrant.core :as ig]))

(defn ^:private exec* [executor entity query]
  (repos/execute! executor
                  query
                  {:entity-fn (crepos/->entity-fn entity)}))

(defn ^:private query-all* [{entity :entity/teams} user-id]
  (q/select-for-user entity user-id))

(defn ^:private query-by-id* [executor {:entity/keys [teams user-teams users]} team-id user-id]
  (let [team (-> executor
                 (exec* teams (log/spy (q/select-one-for-user teams
                                                              team-id
                                                              user-id)))
                 colls/only!)]
    (when team
      (assoc team
             :team/members
             (repos/execute! executor (qu/select-team users
                                                      user-teams
                                                      team-id))))))

(defn ^:private create* [executor {:entity/keys [teams user-teams]} team user-id]
  (let [team-id (-> teams
                    (q/insert (assoc team :created-by user-id))
                    (->> (repos/execute! executor))
                    colls/only!
                    :id)]
    (-> user-teams
        (q/insert-user-team team-id user-id)
        (->> (repos/execute! executor)))
    (-> teams
        (q/select-one team-id)
        (->> (exec* executor teams))
        colls/only!)))

(deftype TeamAccessor [repo]
  pint/IAccessor
  (query-many [_ opts]
    (repos/transact! repo repos/->exec! query-all* (:user/id opts)))
  (query-one [_ opts]
    (repos/transact! repo query-by-id* (:team/id opts) (:user/id opts)))
  (create! [_ team opts]
    (repos/transact! repo create* team (:user/id opts))))

(defmethod ig/init-key ::model [_ {:keys [repo]}]
  (->TeamAccessor repo))
