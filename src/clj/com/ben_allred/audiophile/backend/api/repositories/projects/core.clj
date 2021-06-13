(ns com.ben-allred.audiophile.backend.api.repositories.projects.core
  (:refer-clojure :exclude [accessor])
  (:require
    [com.ben-allred.audiophile.backend.domain.interactors.protocols :as pint]
    [com.ben-allred.audiophile.backend.api.repositories.core :as repos]
    [com.ben-allred.audiophile.backend.api.repositories.projects.protocols :as pp]
    [com.ben-allred.audiophile.backend.api.repositories.common :as crepos]))

(defn ^:private create* [executor opts]
  (let [project-id (pp/insert-project! executor opts opts)
        project (pp/find-event-project executor project-id)]
    (pp/project-created! executor (:user/id opts) project opts)))

(deftype ProjectAccessor [repo]
  pint/IProjectAccessor
  pint/IAccessor
  (query-many [_ opts]
    (repos/transact! repo pp/select-for-user (:user/id opts) opts))
  (query-one [_ opts]
    (repos/transact! repo pp/find-by-project-id (:project/id opts) opts))
  (create! [_ opts]
    (crepos/command! repo opts
      (repos/transact! repo create* opts))))

(defn accessor
  "Constructor for [[ProjectAccessor]] which provides semantic access for storing and retrieving projects."
  [{:keys [repo]}]
  (->ProjectAccessor repo))
