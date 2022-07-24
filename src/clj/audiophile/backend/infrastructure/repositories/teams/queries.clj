(ns audiophile.backend.infrastructure.repositories.teams.queries
  (:require
    [audiophile.backend.infrastructure.db.common :as cdb]
    [audiophile.backend.infrastructure.db.models.core :as models]
    [audiophile.backend.infrastructure.db.models.tables :as tbl]
    [audiophile.backend.infrastructure.repositories.common :as crepos]
    [audiophile.backend.infrastructure.repositories.core :as repos]
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

(defn ^:private select-members* [team-id]
  (-> tbl/users
      (models/alias :member)
      (models/select-fields #{:id :first-name :last-name})
      (models/select* [:= :user-teams.team-id team-id])
      (models/join (-> tbl/user-teams
                       (assoc :namespace :member)
                       (models/select-fields #{:team-id}))
                   [:= :user-teams.user-id :member.id])))

(defn find-by-team-id [executor team-id opts]
  (-> executor
      (repos/execute! (if-let [user-id (:user/id opts)]
                        (select-one-for-user team-id user-id)
                        (models/select-by-id* tbl/teams team-id))
                      (assoc opts :model-fn (crepos/->model-fn tbl/teams)))
      colls/only!))

(defn select-team-members [executor team-id opts]
  (repos/execute! executor
                  (select-members* team-id)
                  opts))

(defn select-for-user [executor user-id opts]
  (repos/execute! executor
                  (models/select* tbl/teams (has-team-clause user-id))
                  (assoc opts :model-fn (crepos/->model-fn tbl/teams))))

(defn insert-team-access? [_ _ _]
  true)

(defn update-team-access? [executor {team-id :team/id} {user-id :user/id}]
  (cdb/access? executor
               (-> tbl/teams
                   (models/select-fields #{:id})
                   (models/select*)
                   (models/join tbl/user-teams [:= :user-teams.team-id :teams.id])
                   (models/and-where [:and
                                      [:= :teams.id team-id]
                                      [:= :user-teams.user-id user-id]]))))

(defn insert-team! [executor team _]
  (let [team-id (-> executor
                    (repos/execute! (models/insert-into tbl/teams team))
                    colls/only!
                    :id)]
    (repos/execute! executor (insert-user-team team-id (:user/id team)))
    team-id))

(defn update-team! [executor team _]
  (repos/execute! executor
                  (-> (models/sql-update tbl/teams)
                      (models/sql-set (select-keys team #{:team/name}))
                      (models/and-where [:= :teams.id (:team/id team)]))))
