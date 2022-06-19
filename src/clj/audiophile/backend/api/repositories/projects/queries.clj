(ns audiophile.backend.api.repositories.projects.queries
  (:require
    [audiophile.backend.api.repositories.core :as repos]
    [audiophile.backend.infrastructure.db.common :as cdb]
    [audiophile.backend.infrastructure.db.models.core :as models]
    [audiophile.backend.infrastructure.db.models.tables :as tbl]
    [audiophile.common.core.utils.colls :as colls]
    [audiophile.common.core.utils.logger :as log]))

(defn ^:private has-team-clause [user-id]
  [:exists (-> tbl/user-teams
               (models/select* [:and
                                [:= :projects.team-id :user-teams.team-id]
                                [:= :user-teams.user-id user-id]])
               (assoc :select [1]))])

(defn ^:private select-one-for-user [project-id user-id]
  (models/select* tbl/projects
                  [:and
                   [:= :projects.id project-id]
                   (has-team-clause user-id)]))

(defn ^:private access-team [team-id user-id]
  (-> tbl/teams
      (models/select* [:and [:= :teams.id team-id]
                       [:= :user-teams.user-id user-id]])
      (models/join tbl/user-teams [:= :user-teams.team-id :teams.id])
      (assoc :select [1])))

(defn find-by-project-id [executor project-id opts]
  (-> executor
      (repos/execute! (if-let [user-id (:user/id opts)]
                        (select-one-for-user project-id user-id)
                        (models/select-by-id* tbl/projects project-id))
                      opts)
      colls/only!))

(defn select-for-user [executor user-id opts]
  (repos/execute! executor
                  (models/select* tbl/projects (has-team-clause user-id))
                  opts))

(defn insert-project-access? [executor project opts]
  (cdb/access? executor (access-team (:project/team-id project) (:user/id opts))))

(defn insert-project! [executor project _]
  (-> executor
      (repos/execute! (models/insert-into tbl/projects project))
      colls/only!
      :id))

(defn find-event-project [executor project-id]
  (-> executor
      (repos/execute! (models/select-by-id* tbl/projects project-id))
      colls/only!))
