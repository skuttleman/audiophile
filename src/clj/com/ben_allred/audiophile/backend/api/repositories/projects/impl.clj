(ns com.ben-allred.audiophile.backend.api.repositories.projects.impl
  (:refer-clojure :exclude [accessor])
  (:require
    [com.ben-allred.audiophile.backend.api.repositories.core :as repos]
    [com.ben-allred.audiophile.backend.api.repositories.projects.core :as rprojects]
    [com.ben-allred.audiophile.backend.domain.interactors.protocols :as pint]
    [com.ben-allred.audiophile.backend.infrastructure.pubsub.core :as ps]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]))

(deftype ProjectAccessor [repo pubsub]
  pint/IProjectAccessor
  pint/IAccessor
  (query-many [_ opts]
    (repos/transact! repo rprojects/select-for-user (:user/id opts) opts))
  (query-one [_ opts]
    (repos/transact! repo rprojects/find-by-project-id (:project/id opts) opts))
  (create! [_ data opts]
    (ps/emit-command! pubsub (:user/id opts) :project/create! data opts)))

(defn accessor
  "Constructor for [[ProjectAccessor]] which provides semantic access for storing and retrieving projects."
  [{:keys [pubsub repo]}]
  (->ProjectAccessor repo pubsub))
