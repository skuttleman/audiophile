(ns audiophile.backend.infrastructure.repositories.team-invitations.queries
  (:require
    [audiophile.backend.infrastructure.db.models.core :as models]
    [audiophile.backend.infrastructure.db.models.sql :as sql]
    [audiophile.backend.infrastructure.db.models.tables :as tbl]
    [audiophile.backend.infrastructure.repositories.common :as crepos]
    [audiophile.backend.infrastructure.repositories.core :as repos]
    [audiophile.common.core.utils.colls :as colls]
    [audiophile.common.core.utils.logger :as log]))

(defn ^:private ->status [status]
  (sql/cast (name status) :team-invitation-status))

(def ^:private ^:const PENDING
  (->status :PENDING))

(def ^:private ^:const REJECTED
  (->status :REJECTED))

(defn find-for-email [executor team-id email]
  (-> executor
      (repos/execute! (-> tbl/team-invitations
                          models/select*
                          (models/and-where [:and
                                             [:= :team-invitations.team-id team-id]
                                             [:= :team-invitations.email email]])))
      colls/only!))

(defn find-for-user [executor team-id user-id]
  (-> executor
      (repos/execute! (-> tbl/team-invitations
                          models/select*
                          (models/join (-> tbl/users
                                           (models/select-fields #{:id}))
                                       [:= :users.email :team-invitations.email])
                          (models/and-where [:and
                                             [:= :users.id user-id]
                                             [:= :team-invitations.team-id team-id]])))
      colls/only!))

(defn invite-member-access? [executor {email :user/email team-id :team/id} {user-id :user/id}]
  (let [sub-query (-> tbl/user-teams
                      (models/alias :sub)
                      models/select*
                      (models/join tbl/users [:= :users.id :sub.user-id])
                      (assoc :select [1])
                      (models/and-where [:and
                                         [:= :sub.team-id team-id]
                                         [:= :users.email email]]))
        query (-> tbl/user-teams
                  models/select*
                  (models/join tbl/teams
                               [:= :teams.id :user-teams.team-id])
                  (assoc :select [1])
                  (models/and-where [:and
                                     [:= :user-teams.team-id team-id]
                                     [:= :user-teams.user-id user-id]
                                     [:not= :teams.type (sql/cast "PERSONAL" :team-type)]
                                     [:not [:exists sub-query]]]))]
    (-> executor
        (repos/execute! query)
        seq)))


(defn invite-member! [executor params opts]
  (let [invited-by (:team-invitation/invited-by params)
        query (-> tbl/team-invitations
                  (models/insert-into params)
                  (models/on-conflict-do-update [:team-id :email]
                                                {:invited-by invited-by
                                                 :status     [:case
                                                              [:and
                                                               [:= :team-invitations.invited-by invited-by]
                                                               [:= :team-invitations.status REJECTED]]
                                                              REJECTED

                                                              :else
                                                              PENDING]}))]
    (repos/execute! executor query opts)))

(defn update-invitation! [executor {:team-invitation/keys [user-id email status team-id]} opts]
  (when (= :ACCEPTED status)
    (-> executor
        (repos/execute! (-> tbl/user-teams
                            (models/insert-into {:user-id user-id
                                                 :team-id team-id})
                            (models/on-conflict-do-nothing [:team-id :user-id]))
                        opts)))
  (-> executor
      (repos/execute! (-> tbl/team-invitations
                          models/sql-update
                          (models/sql-set {:status (->status status)})
                          (models/and-where [:and
                                             [:= :team-invitations.team-id team-id]
                                             [:= :team-invitations.email email]]))
                      opts)))

(defn select-for-user [executor user-id opts]
  (let [query (-> tbl/team-invitations
                  (models/select-fields #{:created-at})
                  models/select*
                  (models/join (models/select-fields tbl/users #{})
                               [:= :users.email :team-invitations.email])
                  (models/join (-> tbl/users
                                   (models/alias :inviter)
                                   (models/select-fields #{:id :first-name :last-name}))
                               [:= :inviter.id :team-invitations.invited-by])
                  (models/join tbl/teams
                               [:= :teams.id :team-invitations.team-id])
                  (models/and-where [:and
                                     [:= :users.id user-id]
                                     [:= :team-invitations.status PENDING]]))]
    (repos/execute! executor query (assoc opts :model-fn (crepos/->model-fn tbl/teams)))))

(defn select-for-team [executor team-id opts]
  (let [query (-> tbl/team-invitations
                  (models/select-fields #{:created-at :email})
                  models/select*
                  (models/join (-> tbl/users
                                   (models/alias :inviter)
                                   (models/select-fields #{:id :first-name :last-name}))
                               [:= :inviter.id :team-invitations.invited-by])
                  (models/and-where [:and
                                     [:= :team-invitations.team-id team-id]
                                     [:= :team-invitations.status PENDING]]))]
    (repos/execute! executor query opts)))
