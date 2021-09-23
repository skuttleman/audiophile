(ns com.ben-allred.audiophile.backend.api.repositories.teams.impl
  (:refer-clojure :exclude [accessor])
  (:require
    [com.ben-allred.audiophile.backend.api.repositories.common :as crepos]
    [com.ben-allred.audiophile.backend.api.repositories.core :as repos]
    [com.ben-allred.audiophile.backend.api.repositories.teams.core :as rteams]
    [com.ben-allred.audiophile.backend.domain.interactors.core :as int]
    [com.ben-allred.audiophile.backend.domain.interactors.protocols :as pint]
    [com.ben-allred.audiophile.backend.infrastructure.pubsub.core :as ps]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]))

(defn ^:private query-by-id* [executor team-id opts]
  (when-let [team (rteams/find-by-team-id executor team-id opts)]
    (assoc team
           :team/members
           (rteams/select-team-members executor team-id opts))))

(defn ^:private create* [executor team opts]
  (if (rteams/insert-team-access? executor team opts)
    (let [team-id (rteams/insert-team! executor team opts)]
      (rteams/find-event-team executor team-id))
    (throw (ex-info "insufficient access" {}))))

(deftype TeamAccessor [repo]
  pint/ITeamAccessor
  pint/IAccessor
  (query-many [_ opts]
    (repos/transact! repo rteams/select-for-user (:user/id opts) opts))
  (query-one [_ opts]
    (repos/transact! repo query-by-id* (:team/id opts) opts))
  (create! [_ data opts]
    (repos/transact! repo create* data opts)))

(defn accessor
  "Constructor for [[TeamAccessor]] which provides semantic access for storing and retrieving teams."
  [{:keys [repo]}]
  (->TeamAccessor repo))

(defn command-handler [{:keys [accessor pubsub]}]
  (letfn [(predicate [{[_ {:command/keys [type]}] :msg}]
            (= :team/create! type))
          (handler [{[_ command ctx] :msg}]
            (let [team (int/create! accessor (:command/data command) ctx)]
              (ps/emit-event! pubsub (:user/id ctx) (:team/id team) :team/created team ctx)))]
    (crepos/command-handler pubsub predicate "saving team to db" handler)))
