(ns audiophile.backend.api.repositories.projects.impl
  (:refer-clojure :exclude [accessor])
  (:require
    [audiophile.backend.api.repositories.core :as repos]
    [audiophile.backend.api.repositories.projects.core :as rprojects]
    [audiophile.backend.domain.interactors.protocols :as pint]
    [audiophile.backend.api.pubsub.core :as ps]
    [audiophile.common.core.utils.logger :as log]))

(deftype ProjectAccessor [repo ch]
  pint/IProjectAccessor
  pint/IAccessor
  (query-many [_ opts]
    (repos/transact! repo rprojects/select-for-user (:user/id opts) opts))
  (query-one [_ opts]
    (repos/transact! repo rprojects/find-by-project-id (:project/id opts) opts))
  (create! [_ data opts]
    (ps/emit-command! ch :project/create! data opts)))

(defn accessor
  "Constructor for [[ProjectAccessor]] which provides semantic access for storing and retrieving projects."
  [{:keys [ch repo]}]
  (->ProjectAccessor repo ch))