(ns audiophile.backend.infrastructure.repositories.projects.impl
  (:refer-clojure :exclude [accessor])
  (:require
    [audiophile.backend.api.pubsub.core :as ps]
    [audiophile.backend.domain.interactors.protocols :as pint]
    [audiophile.backend.infrastructure.repositories.core :as repos]
    [audiophile.backend.infrastructure.repositories.projects.queries :as q]
    [audiophile.backend.infrastructure.templates.workflows :as wf]
    [audiophile.common.core.utils.logger :as log]))

(defmethod wf/with-workflow :projects/create
  [_]
  '{:ctx                {:project/team-id ?team-id
                         :project/name    ?name}
    :workflows/->result {:project/id (sp.ctx/get ?project-id)}})

(deftype ProjectAccessor [repo ch]
  pint/IProjectAccessor
  pint/IAccessor
  (query-many [_ opts]
    (repos/transact! repo q/select-for-user (:user/id opts) opts))
  (query-one [_ opts]
    (repos/transact! repo q/find-by-project-id (:project/id opts) opts))
  (create! [_ data opts]
    (ps/start-workflow! ch :projects/create (merge opts data) opts)))

(defn accessor
  "Constructor for [[ProjectAccessor]] which provides semantic access for storing and retrieving projects."
  [{:keys [ch repo]}]
  (->ProjectAccessor repo ch))
