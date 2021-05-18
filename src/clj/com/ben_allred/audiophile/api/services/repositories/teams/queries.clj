(ns com.ben-allred.audiophile.api.services.repositories.teams.queries
  (:require
    [com.ben-allred.audiophile.api.services.repositories.models.core :as models]))

(defn ^:private has-team-clause [user-id]
  [:exists {:select [:user-id]
            :from   [:user-teams]
            :where  [:and
                     [:= :user-teams.team-id :teams.id]
                     [:= :user-teams.user-id user-id]]}])

(defn select-by [model clause]
  (models/select* model clause))

(defn select-one [model team-id]
  (select-by model [:= :teams.id team-id]))

(defn select-for-user [model user-id]
  (select-by model (has-team-clause user-id)))

(defn select-one-for-user [model team-id user-id]
  (select-by model [:and
                    [:= :teams.id team-id]
                    (has-team-clause user-id)]))

(defn insert [model value]
  (models/insert-into model value))

(defn insert-user-team [model team-id user-id]
  (models/insert-into model {:user-id user-id :team-id team-id}))
