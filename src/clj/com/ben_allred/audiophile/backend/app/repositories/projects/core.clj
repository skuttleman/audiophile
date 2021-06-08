(ns com.ben-allred.audiophile.backend.app.repositories.projects.core
  (:refer-clojure :exclude [accessor])
  (:require
    [com.ben-allred.audiophile.backend.domain.interactors.protocols :as pint]
    [com.ben-allred.audiophile.backend.app.repositories.core :as repos]
    [com.ben-allred.audiophile.backend.app.repositories.projects.protocols :as pp]))

(defn ^:private create* [executor opts]
  (let [project-id (pp/insert-project! executor opts opts)]
    (pp/find-by-project-id executor project-id nil)))

(deftype ProjectAccessor [repo]
  pint/IProjectAccessor
  pint/IAccessor
  (query-many [_ opts]
    (repos/transact! repo pp/select-for-user (:user/id opts) opts))
  (query-one [_ opts]
    (repos/transact! repo pp/find-by-project-id (:project/id opts) opts))
  (create! [_ opts]
    (repos/transact! repo create* opts)))

(defn accessor [{:keys [repo]}]
  (->ProjectAccessor repo))
