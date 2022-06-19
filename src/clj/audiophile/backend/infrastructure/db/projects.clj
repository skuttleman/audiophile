(ns audiophile.backend.infrastructure.db.projects
  (:require
    [audiophile.backend.api.repositories.core :as repos]
    [audiophile.backend.api.repositories.projects.protocols :as pp]
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

(deftype ProjectsRepoExecutor [executor]
  pp/IProjectsExecutor
  (find-by-project-id [_ project-id opts]
    (-> executor
        (repos/execute! (if-let [user-id (:user/id opts)]
                          (select-one-for-user project-id user-id)
                          (models/select-by-id* tbl/projects project-id))
                        opts)
        colls/only!))
  (select-for-user [_ user-id opts]
    (repos/execute! executor
                    (models/select* tbl/projects (has-team-clause user-id))
                    opts))
  (insert-project-access? [_ project opts]
    (cdb/access? executor (access-team (:project/team-id project) (:user/id opts))))
  (insert-project! [_ project _]
    (-> executor
        (repos/execute! (models/insert-into tbl/projects project))
        colls/only!
        :id))
  (find-event-project [_ project-id]
    (-> executor
        (repos/execute! (models/select-by-id* tbl/projects project-id))
        colls/only!)))

(defn ->project-executor
  "Factory function for creating [[ProjectsRepoExecutor]] which provide access to the project repository."
  [_]
  ->ProjectsRepoExecutor)
