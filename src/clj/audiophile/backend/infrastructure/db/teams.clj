(ns audiophile.backend.infrastructure.db.teams
  (:require
    [audiophile.backend.api.repositories.common :as crepos]
    [audiophile.backend.api.repositories.core :as repos]
    [audiophile.backend.api.repositories.teams.protocols :as pt]
    [audiophile.backend.infrastructure.db.models.core :as models]
    [audiophile.backend.infrastructure.db.models.tables :as tbl]
    [audiophile.common.core.utils.colls :as colls]
    [audiophile.common.core.utils.logger :as log]))

(defn ^:private has-team-clause [user-id]
  [:exists (-> tbl/user-teams
               (models/select* [:and
                                [:= :user-teams.team-id :teams.id]
                                [:= :user-teams.user-id user-id]])
               (assoc :select [1]))])

(defn ^:private select-one-for-user [team-id user-id]
  (models/select* tbl/teams
                  [:and
                   [:= :teams.id team-id]
                   (has-team-clause user-id)]))

(defn ^:private insert-user-team [team-id user-id]
  (models/insert-into tbl/user-teams {:user-id user-id
                                      :team-id team-id}))

(defn ^:private select-team [team-id]
  (-> tbl/users
      (models/alias :member)
      (models/select-fields #{:id :first-name :last-name})
      (models/select* [:= :user-teams.team-id team-id])
      (models/join (-> tbl/user-teams
                       (assoc :namespace :member)
                       (models/select-fields #{:team-id}))
                   [:= :user-teams.user-id :member.id])))

(defn ^:private team-repo-executor#insert-team!
  [executor team {user-id :user/id}]
  (let [team-id (-> executor
                    (repos/execute! (models/insert-into tbl/teams team))
                    colls/only!
                    :id)]
    (repos/execute! executor (insert-user-team team-id user-id))
    team-id))

(deftype TeamsRepoExecutor [executor]
  pt/ITeamsExecutor
  (find-by-team-id [_ team-id opts]
    (-> executor
        (repos/execute! (if-let [user-id (:user/id opts)]
                          (select-one-for-user team-id user-id)
                          (models/select-by-id* tbl/teams team-id)))
        colls/only!))
  (select-team-members [_ team-id opts]
    (repos/execute! executor
                    (select-team team-id)
                    opts))
  (select-for-user [_ user-id opts]
    (repos/execute! executor
                    (models/select* tbl/teams (has-team-clause user-id))
                    (assoc opts :model-fn (crepos/->model-fn tbl/teams))))
  (insert-team-access? [_ _ _]
    true)
  (insert-team! [_ team opts]
    (team-repo-executor#insert-team! executor team opts))
  (find-event-team [_ team-id]
    (-> executor
        (repos/execute! (models/select-by-id* tbl/teams team-id))
        colls/only!)))

(defn ->team-executor
  "Factory function for creating [[TeamsRepoExecutor]] which provide access to the team repository."
  [_]
  ->TeamsRepoExecutor)
