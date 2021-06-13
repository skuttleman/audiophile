(ns com.ben-allred.audiophile.backend.infrastructure.db.teams
  (:require
    [com.ben-allred.audiophile.backend.api.repositories.common :as crepos]
    [com.ben-allred.audiophile.backend.api.repositories.core :as repos]
    [com.ben-allred.audiophile.backend.api.repositories.teams.protocols :as pt]
    [com.ben-allred.audiophile.backend.domain.interactors.protocols :as pint]
    [com.ben-allred.audiophile.backend.infrastructure.db.common :as cdb]
    [com.ben-allred.audiophile.backend.infrastructure.db.models.core :as models]
    [com.ben-allred.audiophile.common.core.utils.colls :as colls]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]))

(defn ^:private opts* [model]
  {:model-fn (crepos/->model-fn model)})

(defn ^:private has-team-clause [user-teams user-id]
  [:exists (-> user-teams
               (models/select* [:and
                                [:= :user-teams.team-id :teams.id]
                                [:= :user-teams.user-id user-id]])
               (assoc :select [1]))])

(defn ^:private select-one-for-user [teams user-teams team-id user-id]
  (models/select* teams [:and
                         [:= :teams.id team-id]
                         (has-team-clause user-teams user-id)]))

(defn ^:private insert-user-team [model team-id user-id]
  (models/insert-into model {:user-id user-id :team-id team-id}))

(defn ^:private select-team [users user-teams team-id]
  (-> users
      (assoc :alias :member)
      (models/select-fields #{:id :first-name :last-name})
      (models/select* [:= :user-teams.team-id team-id])
      (models/join (-> user-teams
                       (assoc :namespace :member)
                       (models/select-fields #{:team-id}))
                   [:= :user-teams.user-id :member.id])))

(deftype TeamsRepoExecutor [executor teams user-teams users]
  pt/ITeamsExecutor
  (find-by-team-id [_ team-id opts]
    (colls/only! (repos/execute! executor
                                 (if-let [user-id (:user/id opts)]
                                   (select-one-for-user teams user-teams team-id user-id)
                                   (models/select-by-id* teams team-id)))))
  (select-team-members [_ team-id opts]
    (repos/execute! executor
                    (select-team users user-teams team-id)
                    opts))
  (select-for-user [_ user-id opts]
    (repos/execute! executor
                    (models/select* teams (has-team-clause user-teams user-id))
                    (merge opts (opts* teams))))
  (insert-team! [_ team {user-id :user/id}]
    (let [team-id (-> executor
                      (repos/execute! (models/insert-into teams team))
                      colls/only!
                      :id)]
      (repos/execute! executor
                      (insert-user-team user-teams
                                        team-id
                                        user-id))
      team-id))
  (find-event-team [_ team-id]
    (-> executor
        (repos/execute! (models/select-by-id* teams team-id))
        colls/only!)))

(defn ->team-executor
  "Factory function for creating [[TeamsRepoExecutor]] which provide access to the team repository."
  [{:keys [teams user-teams users]}]
  (fn [executor]
    (->TeamsRepoExecutor executor teams user-teams users)))

(deftype TeamsEventEmitter [executor emitter pubsub]
  pt/ITeamsEventEmitter
  (team-created! [_ user-id team ctx]
    (cdb/emit! executor pubsub user-id (:team/id team) :team/created team ctx))

  pint/IEmitter
  (command-failed! [_ request-id opts]
    (pint/command-failed! emitter request-id opts)))

(defn ->team-event-emitter
  "Factory function for creating [[TeamsEventEmitter]] used to emit events related to teams."
  [{:keys [->emitter pubsub]}]
  (fn [executor]
    (->TeamsEventEmitter executor (->emitter executor) pubsub)))

(deftype Executor [executor emitter]
  pt/ITeamsExecutor
  (find-by-team-id [_ team-id opts]
    (pt/find-by-team-id executor team-id opts))
  (select-for-user [_ user-id opts]
    (pt/select-for-user executor user-id opts))
  (select-team-members [_ team-id opts]
    (pt/select-team-members executor team-id opts))
  (insert-team! [_ team opts]
    (pt/insert-team! executor team opts))
  (find-event-team [_ team-id]
    (pt/find-event-team executor team-id))

  pt/ITeamsEventEmitter
  (team-created! [_ user-id team ctx]
    (pt/team-created! emitter user-id team ctx))

  pint/IEmitter
  (command-failed! [_ request-id opts]
    (pint/command-failed! emitter request-id opts)))

(defn ->executor
  "Factory function for creating [[Executor]] which aggregates [[TeamsEventEmitter]]
   and [[TeamsRepoExecutor]]."
  [{:keys [->event-executor ->team-event-emitter ->team-executor]}]
  (fn [executor]
    (->Executor (->team-executor executor)
                (->team-event-emitter (->event-executor executor)))))
