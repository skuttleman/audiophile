(ns com.ben-allred.audiophile.api.services.repositories.users.queries
  (:require
    [com.ben-allred.audiophile.api.services.repositories.entities.core :as entities]))

(defn select-by [entity clause]
  (-> entity
      (update :fields (partial remove #{:mobile-number}))
      (entities/select* clause)))

(defn select-team [entity user-teams team-id]
  (-> entity
      (assoc :alias :member)
      (entities/select-fields #{:id :first-name :last-name})
      (select-by [:= :user-teams.team-id team-id])
      (entities/join (-> user-teams
                         (assoc :namespace :member)
                         (entities/select-fields #{:team-id}))
                     [:= :user-teams.user-id :member.id])))
