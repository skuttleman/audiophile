(ns com.ben-allred.audiophile.backend.api.repositories.projects.impl
  (:refer-clojure :exclude [accessor])
  (:require
    [com.ben-allred.audiophile.backend.domain.interactors.protocols :as pint]
    [com.ben-allred.audiophile.backend.api.repositories.core :as repos]
    [com.ben-allred.audiophile.backend.api.repositories.common :as crepos]
    [com.ben-allred.audiophile.backend.api.repositories.projects.core :as rprojects]))

(defn ^:private create* [executor data opts]
  (let [project-id (rprojects/insert-project! executor data opts)
        project (rprojects/find-event-project executor project-id)]
    (rprojects/project-created! executor (:user/id opts) project opts)))

(deftype ProjectAccessor [repo]
  pint/IProjectAccessor
  pint/IAccessor
  (query-many [_ opts]
    (repos/transact! repo rprojects/select-for-user (:user/id opts) opts))
  (query-one [_ opts]
    (repos/transact! repo rprojects/find-by-project-id (:project/id opts) opts))
  (create! [_ data opts]
    (crepos/command! repo opts
      (repos/transact! repo create* data opts))))

(defn accessor
  "Constructor for [[ProjectAccessor]] which provides semantic access for storing and retrieving projects."
  [{:keys [repo]}]
  (->ProjectAccessor repo))
