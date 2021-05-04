(ns com.ben-allred.audiophile.api.services.repositories.teams.model
  (:require
    [com.ben-allred.audiophile.api.services.repositories.common :as crepos]
    [com.ben-allred.audiophile.api.services.repositories.core :as repos]
    [com.ben-allred.audiophile.api.services.repositories.entities.core :as entities]
    [com.ben-allred.audiophile.api.services.repositories.teams.queries :as qteams]
    [com.ben-allred.audiophile.api.services.repositories.users.queries :as qusers]
    [com.ben-allred.audiophile.common.utils.colls :as colls]))

(defn ^:private exec* [executor entity query]
  (repos/execute! executor
                  query
                  {:entity-fn (crepos/->entity-fn entity)}))

(defn query-all [repo user-id]
  (repos/transact! repo
                   (fn [executor {entity :entity/teams}]
                     (-> entity
                         (qteams/select-for-user user-id)
                         (->> (exec* executor entity))))))

(defn query-by-id [repo team-id user-id]
  (repos/transact! repo
                   (fn [executor {:entity/keys [teams user-teams users]}]
                     (let [team (-> executor
                                    (exec* teams (qteams/select-one-for-user teams
                                                                             team-id
                                                                             user-id))
                                    colls/only!)]
                       (assoc team
                              :team/members
                              (repos/execute! executor
                                              (-> users
                                                  (assoc :alias :member)
                                                  (entities/select-fields #{:id :first-name :last-name})
                                                  (qusers/select-by [:= :user-teams.team-id team-id])
                                                  (entities/join (-> user-teams
                                                                     (assoc :namespace :member)
                                                                     (entities/select-fields #{:team-id}))
                                                                 [:= :user-teams.user-id :member.id]))))))))

(defn create! [repo team user-id]
  (repos/transact! repo
                   (fn [executor {:entity/keys [teams user-teams]}]
                     (let [team-id (-> teams
                                       (qteams/insert (assoc team :created-by user-id))
                                       (->> (repos/execute! executor))
                                       colls/only!
                                       :id)]
                       (-> user-teams
                           (entities/insert-into {:user-id user-id :team-id team-id})
                           (->> (repos/execute! executor)))
                       (-> teams
                           (qteams/select-one team-id)
                           (->> (exec* executor teams))
                           colls/only!)))))
