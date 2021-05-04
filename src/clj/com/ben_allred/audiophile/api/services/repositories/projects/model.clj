(ns com.ben-allred.audiophile.api.services.repositories.projects.model
  (:require
    [com.ben-allred.audiophile.api.services.repositories.core :as repos]
    [com.ben-allred.audiophile.api.services.repositories.projects.queries :as qprojects]
    [com.ben-allred.audiophile.common.utils.colls :as colls]
    [com.ben-allred.audiophile.common.utils.fns :as fns]))

(defn query-all
  "Returns all projects for a user."
  [repo user-id]
  (repos/transact! repo repos/->exec!
                   (fns/=> :entity/projects
                           (qprojects/select-for-user user-id))))

(defn query-by-id
  "Returns a single project for a user by the project-id."
  [repo project-id user-id]
  (-> repo
      (repos/transact! repos/->exec!
                       (fns/=> :entity/projects
                               (qprojects/select-one-for-user project-id user-id)))
      colls/only!))

(defn create!
  "Creates a project and returns the created entity."
  [repo project user-id]
  (repos/transact! repo (fn [executor {entity :entity/projects}]
                          (let [project-id (-> entity
                                               (qprojects/insert (assoc project :created-by user-id))
                                               (->> (repos/execute! executor))
                                               colls/only!
                                               :id)]
                            (repos/execute! executor
                                            (qprojects/select-one entity project-id))))))
