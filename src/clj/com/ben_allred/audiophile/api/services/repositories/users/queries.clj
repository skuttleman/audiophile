(ns com.ben-allred.audiophile.api.services.repositories.users.queries
  (:require
    [com.ben-allred.audiophile.api.services.repositories.models.core :as models]))

(defn select-by [models clause]
  (-> models
      (update :fields (partial remove #{:mobile-number}))
      (models/select* clause)))

(defn select-team [models user-teams team-id]
  (-> models
      (assoc :alias :member)
      (models/select-fields #{:id :first-name :last-name})
      (select-by [:= :user-teams.team-id team-id])
      (models/join (-> user-teams
                         (assoc :namespace :member)
                         (models/select-fields #{:team-id}))
                     [:= :user-teams.user-id :member.id])))
