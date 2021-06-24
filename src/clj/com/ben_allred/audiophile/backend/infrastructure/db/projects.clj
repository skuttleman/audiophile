(ns com.ben-allred.audiophile.backend.infrastructure.db.projects
  (:require
    [com.ben-allred.audiophile.backend.api.repositories.core :as repos]
    [com.ben-allred.audiophile.backend.api.repositories.projects.protocols :as pp]
    [com.ben-allred.audiophile.backend.domain.interactors.protocols :as pint]
    [com.ben-allred.audiophile.backend.infrastructure.db.common :as cdb]
    [com.ben-allred.audiophile.backend.infrastructure.db.models.core :as models]
    [com.ben-allred.audiophile.common.core.utils.colls :as colls]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]))

(defn ^:private has-team-clause [user-teams user-id]
  [:exists (-> user-teams
               (models/select* [:and
                                [:= :projects.team-id :user-teams.team-id]
                                [:= :user-teams.user-id user-id]])
               (assoc :select [1]))])

(defn ^:private select-one-for-user [projects project-id user-teams user-id]
  (models/select* projects [:and
                            [:= :projects.id project-id]
                            (has-team-clause user-teams user-id)]))

(defn ^:private access-team [teams user-teams team-id user-id]
  (-> teams
      (models/select* [:and [:= :teams.id team-id]
                       [:= :user-teams.user-id user-id]])
      (models/join user-teams [:= :user-teams.team-id :teams.id])
      (assoc :select [1])))

(deftype ProjectsRepoExecutor [executor projects teams user-teams users]
  pp/IProjectsExecutor
  (find-by-project-id [_ project-id opts]
    (colls/only! (repos/execute! executor
                                 (if-let [user-id (:user/id opts)]
                                   (select-one-for-user projects project-id user-teams user-id)
                                   (models/select-by-id* projects project-id))
                                 opts)))
  (select-for-user [_ user-id opts]
    (repos/execute! executor
                    (models/select* projects (has-team-clause user-teams user-id))
                    opts))
  (insert-project-access? [_ project opts]
    (cdb/access? executor (access-team teams user-teams (:project/team-id project) (:user/id opts))))
  (insert-project! [_ project _]
    (-> executor
        (repos/execute! (models/insert-into projects project))
        colls/only!
        :id))
  (find-event-project [_ project-id]
    (-> executor
        (repos/execute! (models/select-by-id* projects project-id))
        colls/only!)))

(defn ->project-executor
  "Factory function for creating [[ProjectsRepoExecutor]] which provide access to the project repository."
  [{:keys [projects teams user-teams users]}]
  (fn [executor]
    (->ProjectsRepoExecutor executor projects teams user-teams users)))

(deftype ProjectsEventEmitter [executor emitter pubsub]
  pp/IProjectsEventEmitter
  (project-created! [_ user-id project ctx]
    (cdb/emit! executor pubsub user-id (:project/id project) :project/created project ctx))

  pint/IEmitter
  (command-failed! [_ request-id opts]
    (pint/command-failed! emitter request-id opts)))

(defn ->project-event-emitter
  "Factory function for creating [[ProjectsEventEmitter]] used to emit events related to projects."
  [{:keys [->emitter pubsub]}]
  (fn [executor]
    (->ProjectsEventEmitter executor (->emitter executor) pubsub)))

(deftype Executor [executor emitter]
  pp/IProjectsExecutor
  (find-by-project-id [_ project-id opts]
    (pp/find-by-project-id executor project-id opts))
  (select-for-user [_ user-id opts]
    (pp/select-for-user executor user-id opts))
  (insert-project-access? [_ project opts]
    (pp/insert-project-access? executor project opts))
  (insert-project! [_ project opts]
    (pp/insert-project! executor project opts))
  (find-event-project [_ project-id]
    (pp/find-event-project executor project-id))

  pp/IProjectsEventEmitter
  (project-created! [_ user-id project ctx]
    (pp/project-created! emitter user-id project ctx))

  pint/IEmitter
  (command-failed! [_ request-id opts]
    (pint/command-failed! emitter request-id opts)))

(defn ->executor
  "Factory function for creating [[Executor]] which aggregates [[ProjectsEventEmitter]]
   and [[ProjectsRepoExecutor]]."
  [{:keys [->event-executor ->project-event-emitter ->project-executor]}]
  (fn [executor]
    (->Executor (->project-executor executor)
                (->project-event-emitter (->event-executor executor)))))
