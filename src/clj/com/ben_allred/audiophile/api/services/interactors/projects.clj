(ns com.ben-allred.audiophile.api.services.interactors.projects
  (:require
    [com.ben-allred.audiophile.api.services.interactors.common :as int]
    [com.ben-allred.audiophile.api.services.repositories.core :as repos]
    [com.ben-allred.audiophile.api.services.repositories.projects :as projects]
    [com.ben-allred.audiophile.common.utils.colls :as colls]
    [com.ben-allred.audiophile.common.utils.fns :as fns]))

(defn query-all
  "Returns all projects for a user."
  [repo user-id]
  (when-not user-id
    (int/missing-user-ctx!))
  (repos/transact! repo repos/->exec!
                   (fns/=> :entity/projects
                           (projects/select-for-user user-id))))

(defn query-by-id
  "Returns a single project for a user by the project-id."
  [repo project-id user-id]
  (when-not user-id
    (int/missing-user-ctx!))
  (-> repo
      (repos/transact! repos/->exec!
                       (fns/=> :entity/projects
                               (projects/select-one-for-user project-id user-id)))
      colls/only!))

(defn create!
  "Creates a project and returns the created entity."
  [repo project user-id]
  (when-not user-id
    (int/missing-user-ctx!))
  (-> repo
      (repos/transact! (fn [executor {entity :entity/projects}]
                         (let [project-id (-> entity
                                              (projects/insert project user-id)
                                              (->> (repos/execute! executor))
                                              colls/only!
                                              :id)]
                           (repos/execute! executor
                                           (projects/select-one entity project-id)))))
      colls/only!))
