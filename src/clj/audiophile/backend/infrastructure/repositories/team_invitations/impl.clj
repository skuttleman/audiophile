(ns audiophile.backend.infrastructure.repositories.team-invitations.impl
  (:refer-clojure :exclude [accessor])
  (:require
    [audiophile.backend.domain.interactors.core :as int]
    [audiophile.backend.domain.interactors.protocols :as pint]
    [audiophile.backend.infrastructure.repositories.common :as crepos]
    [audiophile.backend.infrastructure.repositories.core :as repos]
    [audiophile.backend.infrastructure.repositories.team-invitations.queries :as qinvitations]
    [audiophile.backend.infrastructure.repositories.users.queries :as qusers]
    [audiophile.common.core.utils.logger :as log]))

(defn ^:private team-invite* [executor {:user/keys [email] :as data} opts]
  (when-not (qinvitations/invite-member-access? executor data opts)
    (int/no-access!))
  (merge opts data {:user/id (-> executor
                                 (qusers/find-by-email email opts)
                                 :user/id)
                    :inviter/id (:user/id opts)}))

(defn ^:private update-invite* [executor {email :team-invitation/email :as data} {user-id :user/id :as opts}]
  (let [team-id (:team-invitation/team-id data)
        invitation (if email
                     (qinvitations/find-for-email executor team-id email)
                     (qinvitations/find-for-user executor team-id user-id))]
    (when-not invitation
      (int/no-access!))
    (merge invitation opts data {:team/id team-id})))

(deftype TeamInvitationAccessor [repo producer]
  pint/ITeamInvitationAccessor
  pint/IAccessor
  (create! [_ data opts]
    (let [data (repos/transact! repo team-invite* data opts)]
      (crepos/start-workflow! producer :invitations/create data opts)))
  (update! [_ data opts]
    (let [data (repos/transact! repo update-invite* data opts)]
      (crepos/start-workflow! producer :invitations/update data opts)))
  (query-many [_ opts]
    (repos/transact! repo qinvitations/select-for-user (:user/id opts) opts)))

(defn accessor
  "Constructor for [[TeamInvitationAccessor]] which provides semantic access for storing and retrieving teams."
  [{:keys [producer repo]}]
  (->TeamInvitationAccessor repo producer))
