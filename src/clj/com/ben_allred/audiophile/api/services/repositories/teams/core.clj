(ns com.ben-allred.audiophile.api.services.repositories.teams.core
  (:require
    [com.ben-allred.audiophile.api.services.interactors.core :as int]
    [com.ben-allred.audiophile.api.services.interactors.protocols :as pint]
    [com.ben-allred.audiophile.api.services.repositories.common :as crepos]
    [com.ben-allred.audiophile.api.services.repositories.core :as repos]
    [com.ben-allred.audiophile.api.services.repositories.entities.core :as entities]
    [com.ben-allred.audiophile.api.services.repositories.teams.queries :as q]
    [com.ben-allred.audiophile.api.services.repositories.users.queries :as qu]
    [com.ben-allred.audiophile.common.utils.colls :as colls]
    [integrant.core :as ig]))

(defn ^:private exec* [executor entity query]
  (repos/execute! executor
                  query
                  {:entity-fn (crepos/->entity-fn entity)}))

(defn ^:private query-all* [{entity :entity/teams} user-id]
  (when-not user-id
    (int/missing-user-ctx!))
  (q/select-for-user entity user-id))

(defn ^:private query-by-id* [executor {:entity/keys [teams user-teams users]} team-id user-id]
  (when-not user-id
    (int/missing-user-ctx!))
  (let [team (-> executor
                 (exec* teams (q/select-one-for-user teams
                                                     team-id
                                                     user-id))
                 colls/only!)]
    (assoc team
           :team/members
           (repos/execute! executor
                           (-> users
                               (assoc :alias :member)
                               (entities/select-fields #{:id :first-name :last-name})
                               (qu/select-by [:= :user-teams.team-id team-id])
                               (entities/join (-> user-teams
                                                  (assoc :namespace :member)
                                                  (entities/select-fields #{:team-id}))
                                              [:= :user-teams.user-id :member.id]))))))

(defn ^:private create* [executor {:entity/keys [teams user-teams]} team user-id]
  (when-not user-id
    (int/missing-user-ctx!))
  (let [team-id (-> teams
                    (q/insert (assoc team :created-by user-id))
                    (->> (repos/execute! executor))
                    colls/only!
                    :id)]
    (-> user-teams
        (entities/insert-into {:user-id user-id :team-id team-id})
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
