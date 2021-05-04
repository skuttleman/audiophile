(ns com.ben-allred.audiophile.api.services.repositories.teams.queries
  (:require [com.ben-allred.audiophile.api.services.repositories.entities.core :as entities]))

(defn ^:private has-team-clause [user-id]
  [:exists {:select [:user-id]
            :from   [:user-teams]
            :where  [:and
                     [:= :user-teams.team-id :teams.id]
                     [:= :user-teams.user-id user-id]]}])

(defn select-by [entity clause]
  (entities/select* entity clause))

(defn select-one [entity team-id]
  (select-by entity [:= :teams.id team-id]))

(defn select-for-user [entity user-id]
  (select-by entity (has-team-clause user-id)))

(defn select-one-for-user [entity team-id user-id]
  (select-by entity [:and
                     [:= :teams.id team-id]
                     (has-team-clause user-id)]))

(defn insert [entity value]
  (entities/insert-into entity value))
