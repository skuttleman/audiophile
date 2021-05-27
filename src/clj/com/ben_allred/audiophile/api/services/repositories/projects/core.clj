(ns com.ben-allred.audiophile.api.services.repositories.projects.core
  (:require
    [com.ben-allred.audiophile.api.services.interactors.protocols :as pint]
    [com.ben-allred.audiophile.api.services.repositories.core :as repos]
    [com.ben-allred.audiophile.api.services.repositories.projects.queries :as q]
    [integrant.core :as ig]))

(defn ^:private create* [executor opts]
  (let [project-id (q/insert-project! executor opts opts)]
    (q/find-by-project-id executor project-id)))

(deftype ProjectAccessor [repo]
  pint/IProjectAccessor
  pint/IAccessor
  (query-many [_ opts]
    (repos/transact! repo q/select-for-user (:user/id opts)))
  (query-one [_ opts]
    (repos/transact! repo q/find-by-project-id (:project/id opts) opts))
  (create! [_ opts]
    (repos/transact! repo create* opts)))

(defmethod ig/init-key ::accessor [_ {:keys [repo]}]
  (->ProjectAccessor repo))
