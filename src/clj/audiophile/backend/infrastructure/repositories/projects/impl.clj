(ns audiophile.backend.infrastructure.repositories.projects.impl
  (:refer-clojure :exclude [accessor])
  (:require
    [audiophile.backend.domain.interactors.protocols :as pint]
    [audiophile.backend.infrastructure.repositories.core :as repos]
    [audiophile.backend.infrastructure.repositories.projects.queries :as qprojects]
    [audiophile.backend.infrastructure.templates.workflows :as wf]
    [audiophile.common.core.utils.logger :as log]))

(deftype ProjectAccessor [repo producer]
  pint/IProjectAccessor
  pint/IAccessor
  (query-many [_ opts]
    (repos/transact! repo qprojects/select-for-user (:user/id opts) opts))
  (query-one [_ opts]
    (repos/transact! repo qprojects/find-by-project-id (:project/id opts) opts))
  (create! [_ data opts]
    (wf/start-workflow! producer :projects/create (merge opts data) opts)))

(defn accessor
  "Constructor for [[ProjectAccessor]] which provides semantic access for storing and retrieving projects."
  [{:keys [producer repo]}]
  (->ProjectAccessor repo producer))
