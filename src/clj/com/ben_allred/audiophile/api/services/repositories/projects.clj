(ns com.ben-allred.audiophile.api.services.repositories.projects
  (:require
    [com.ben-allred.audiophile.api.services.repositories.common :as crepos]
    [com.ben-allred.audiophile.api.services.repositories.core :as repos]
    [com.ben-allred.audiophile.common.utils.maps :as maps]))

(defn ^:private clause* [user-id]
  [:exists {:select [:user-id]
            :from   [:user-teams]
            :where  [:= :user-teams.user-id user-id]}])

(defn query-all
  "Returns all projects for a user."
  [repo user-id]
  (crepos/query-many repo :entity/projects (clause* user-id)))

(defn query-by-id
  "Returns a single project for a user by the project-id."
  [repo project-id user-id]
  (crepos/query-one repo :entity/projects [:and
                                           [:= :id project-id]
                                           (clause* user-id)]))

(defn create!
  "Creates a project and returns the created entity."
  [repo project user-id]
  (crepos/create! repo :entity/projects (assoc project :created-by user-id)))

(defn access! [executor project-id user-id]
  (or (first (repos/execute! executor {:select [:id]
                                       :from   [:projects]
                                       :join   [:user-teams [:= :user-teams.team-id :projects.team-id]]
                                       :where  [:and
                                                [:= :projects.id project-id]
                                                [:= :user-teams.user-id user-id]]}))
      (throw (ex-info "cannot access this project" (maps/->m project-id user-id)))))
