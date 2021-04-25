(ns com.ben-allred.audiophile.api.services.repositories.projects
  (:require
    [com.ben-allred.audiophile.api.services.repositories.common :as crepos]))

(defn clause* [user-id]
  [:exists {:select [:user-id]
            :from   [:user-teams]
            :where  [:= :user-teams.user-id user-id]}])

(defn query-all [repo user-id]
  (crepos/query-many repo :entity/projects (clause* user-id)))

(defn query-by-id [repo project-id user-id]
  (crepos/query-one repo :entity/projects [:and
                                           [:= :id project-id]
                                           (clause* user-id)]))

(defn create! [repo project user-id]
  (crepos/create! repo :entity/projects (assoc project :created-by user-id)))
