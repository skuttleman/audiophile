(ns com.ben-allred.audiophile.api.services.repositories.projects.queries
  (:require
    [com.ben-allred.audiophile.api.services.repositories.core :as repos]
    [com.ben-allred.audiophile.api.services.repositories.models.core :as models]
    [com.ben-allred.audiophile.api.services.repositories.projects.protocols :as pp]
    [com.ben-allred.audiophile.common.utils.colls :as colls]))

(defn ^:private has-team-clause [user-teams user-id]
  [:exists (-> user-teams
               (models/select* [:and
                                [:= :projects.team-id :user-teams.team-id]
                                [:= :user-teams.user-id user-id]])
               (assoc :select [1]))])

(defn ^:private select-one [projects project-id]
  (models/select* projects [:= :projects.id project-id]))

(defn ^:private select-one-for-user [projects project-id user-teams user-id]
  (models/select* projects [:and
                            [:= :projects.id project-id]
                            (has-team-clause user-teams user-id)]))

(defn ^:private insert [projects value user-id]
  (models/insert-into projects (assoc value :created-by user-id)))

(deftype ProjectExecutor [executor projects user-teams users]
  pp/IProjectsExecutor
  (find-by-project-id [_ project-id opts]
    (colls/only! (repos/execute! executor
                                 (if-let [user-id (:user/id opts)]
                                   (select-one-for-user projects project-id user-teams user-id)
                                   (select-one projects project-id))
                                 opts)))
  (select-for-user [_ user-id opts]
    (repos/execute! executor
                    (models/select* projects (has-team-clause user-teams user-id))
                    opts))
  (insert-project! [_ project opts]
    (-> executor
        (repos/execute! (insert projects project (:user/id opts)))
        colls/only!
        :id)))

(defn ->executor [{:keys [projects user-teams users]}]
  (fn [executor]
    (->ProjectExecutor executor projects user-teams users)))
