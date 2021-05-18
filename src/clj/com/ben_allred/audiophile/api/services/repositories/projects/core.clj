(ns com.ben-allred.audiophile.api.services.repositories.projects.core
  (:require
    [com.ben-allred.audiophile.api.services.interactors.core :as int]
    [com.ben-allred.audiophile.api.services.interactors.protocols :as pint]
    [com.ben-allred.audiophile.api.services.repositories.core :as repos]
    [com.ben-allred.audiophile.api.services.repositories.projects.queries :as q]
    [com.ben-allred.audiophile.common.utils.colls :as colls]
    [integrant.core :as ig]))

(defn ^:private query-all* [{model :models/projects} user-id]
  (q/select-for-user model user-id))

(defn ^:private query-by-id* [{model :models/projects} project-id user-id]
  (q/select-one-for-user model project-id user-id))

(defn ^:private create* [executor {model :models/projects} project user-id]
  (let [project-id (-> model
                       (q/insert project user-id)
                       (->> (repos/execute! executor))
                       colls/only!
                       :id)]
    (-> executor
        (repos/execute! (q/select-one model project-id))
        colls/only!)))

(deftype ProjectAccessor [repo]
  pint/IAccessor
  (query-many [_ opts]
    (repos/transact! repo repos/->exec! query-all* (:user/id opts)))
  (query-one [_ opts]
    (colls/only! (repos/transact! repo repos/->exec! query-by-id* (:project/id opts) (:user/id opts))))
  (create! [_ opts]
    (repos/transact! repo create* opts (:user/id opts))))

(defmethod ig/init-key ::model [_ {:keys [repo]}]
  (->ProjectAccessor repo))
