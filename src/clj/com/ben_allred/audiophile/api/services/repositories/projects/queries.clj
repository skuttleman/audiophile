(ns com.ben-allred.audiophile.api.services.repositories.projects.queries
  (:require
    [com.ben-allred.audiophile.api.services.repositories.models.core :as models]))

(defn ^:private has-team-clause [user-id]
  [:exists {:select [:user-id]
            :from   [:user-teams]
            :where  [:and
                     [:= :projects.team-id :user-teams.team-id]
                     [:= :user-teams.user-id user-id]]}])

(defn select-by [model clause]
  (models/select* model clause))

(defn select-one [model project-id]
  (select-by model [:= :projects.id project-id]))

(defn select-for-user [model user-id]
  (select-by model (has-team-clause user-id)))

(defn select-one-for-user [model project-id user-id]
  (select-by model [:and
                    [:= :projects.id project-id]
                    (has-team-clause user-id)]))

(defn insert [model value user-id]
  (models/insert-into model (assoc value :created-by user-id)))
