(ns com.ben-allred.audiophile.api.infrastructure.db.teams
  (:require
    [com.ben-allred.audiophile.api.app.repositories.common :as crepos]
    [com.ben-allred.audiophile.api.app.repositories.core :as repos]
    [com.ben-allred.audiophile.api.infrastructure.db.models.core :as models]
    [com.ben-allred.audiophile.api.app.repositories.teams.protocols :as pt]
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

(defn ^:private select-by [model clause]
  (models/select* model clause))

(defn ^:private select-one [model team-id]
  (select-by model [:= :teams.id team-id]))

(defn ^:private select-one-for-user [teams user-teams team-id user-id]
  (select-by teams [:and
                    [:= :teams.id team-id]
                    (has-team-clause user-teams user-id)]))

(defn ^:private insert-user-team [model team-id user-id]
  (models/insert-into model {:user-id user-id :team-id team-id}))

(defn ^:private select-team [model user-teams team-id]
  (-> model
      (assoc :alias :member)
      (models/select-fields #{:id :first-name :last-name})
      (select-by [:= :user-teams.team-id team-id])
      (models/join (-> user-teams
                       (assoc :namespace :member)
                       (models/select-fields #{:team-id}))
                   [:= :user-teams.user-id :member.id])))

(deftype TeamExecutor [executor teams user-teams users]
  pt/ITeamsExecutor
  (find-by-team-id [_ team-id opts]
    (colls/only! (repos/execute! executor
                                 (if-let [user-id (:user/id opts)]
                                   (select-one-for-user teams user-teams team-id user-id)
                                   (select-one teams team-id)))))
  (select-team-members [_ team-id opts]
    (repos/execute! executor
                    (select-team users user-teams team-id)
                    opts))
  (select-for-user [_ user-id opts]
    (repos/execute! executor
                    (select-by teams (has-team-clause user-teams user-id))
                    (merge opts (opts* teams))))
  (insert-team! [_ team {user-id :user/id :as thing}]
    (log/warn thing)
    (let [team-id (-> executor
                      (repos/execute! (models/insert-into teams (assoc team :created-by user-id)))
                      colls/only!
                      :id)]
      (repos/execute! executor
                      (insert-user-team user-teams
                                        team-id
                                        user-id))
      team-id)))

(defn ->executor [{:keys [teams user-teams users]}]
  (fn [executor]
    (->TeamExecutor executor teams user-teams users)))
