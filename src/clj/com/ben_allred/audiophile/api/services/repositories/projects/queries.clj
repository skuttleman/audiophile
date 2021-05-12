(ns com.ben-allred.audiophile.api.services.repositories.projects.queries
  (:require
    [com.ben-allred.audiophile.api.services.repositories.entities.core :as entities]))

(defn ^:private has-team-clause [user-id]
  [:exists {:select [:user-id]
            :from   [:user-teams]
            :where  [:and
                     [:= :projects.team-id :user-teams.team-id]
                     [:= :user-teams.user-id user-id]]}])

(defn select-by [entity clause]
  (entities/select* entity clause))

(defn select-one [entity project-id]
  (select-by entity [:= :projects.id project-id]))

(defn select-for-user [entity user-id]
  (select-by entity (has-team-clause user-id)))

(defn select-one-for-user [entity project-id user-id]
  (select-by entity [:and
                     [:= :projects.id project-id]
                     (has-team-clause user-id)]))

(defn insert [entity value user-id]
  (entities/insert-into entity (assoc value :created-by user-id)))
