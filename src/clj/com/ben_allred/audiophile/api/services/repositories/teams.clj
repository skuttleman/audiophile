(ns com.ben-allred.audiophile.api.services.repositories.teams
  (:require
    [com.ben-allred.audiophile.api.services.repositories.common :as crepos]
    [com.ben-allred.audiophile.api.services.repositories.core :as repos]
    [com.ben-allred.audiophile.api.services.repositories.entities.core :as entities]
    [com.ben-allred.audiophile.common.utils.logger :as log]))

(defn clause* [user-id]
  [:exists {:select [:user-id]
            :from   [:user-teams]
            :where  [:= :user-teams.user-id user-id]}])

(defn query-all [repo user-id]
  (crepos/query-many repo :entity/teams (clause* user-id)))

(defn query-by-id [repo team-id user-id]
  (crepos/query-one repo :entity/teams [:and
                                        [:= :id team-id]
                                        (clause* user-id)]))

(defn create! [repo team user-id]
  (repos/transact! repo (fn [executor {entity :entity/teams}]
                          (let [team-id (crepos/create* executor entity (assoc team :created-by user-id))
                                row {:team-id team-id :user-id user-id}]
                            (repos/execute! executor
                                            (entities/insert-into {:table :user-teams :fields row}
                                                                  row))
                            (->> [:= :id team-id]
                                 (entities/select* entity)
                                 (repos/execute! executor)
                                 first)))))
