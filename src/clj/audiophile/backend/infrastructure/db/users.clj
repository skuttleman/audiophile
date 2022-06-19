(ns audiophile.backend.infrastructure.db.users
  (:require
    [audiophile.backend.api.repositories.core :as repos]
    [audiophile.backend.api.repositories.search.protocols :as ps]
    [audiophile.backend.api.repositories.users.protocols :as pu]
    [audiophile.backend.infrastructure.db.models.core :as models]
    [audiophile.backend.infrastructure.db.models.tables :as tbl]
    [audiophile.common.core.utils.colls :as colls]))

(defn ^:private select-by [clause]
  (-> tbl/users
      (models/select-fields #{:id :first-name :handle})
      (models/select* clause)))

(defn ^:private select!
  ([executor query]
   (select! executor query nil))
  ([executor query opts]
   (-> executor
       (repos/execute! query opts)
       colls/only!)))

(deftype UserExecutor [executor]
  pu/IUserExecutor
  (find-by-id [_ user-id opts]
    (select! executor (models/select* tbl/users [:= :users.id user-id]) opts))
  (find-by-email [_ email opts]
    (select! executor (select-by [:= :users.email email]) opts))
  (insert-user! [_ user _]
    (-> executor
        (select! (models/insert-into tbl/users user))
        :id))

  ps/ISearchUserExecutor
  (find-by-handle [_ handle opts]
    (select! executor
             (-> [:= :users.handle handle]
                 select-by
                 (assoc :select [1]))
             opts))
  (find-by-mobile-number [_ mobile-number opts]
    (select! executor
             (-> [:= :users.mobile-number mobile-number]
                 select-by
                 (assoc :select [1]))
             opts)))

(defn ->executor
  "Factory function for creating [[UserExecutor]] which provides access to the user repository
   inside a transaction."
  [_]
  ->UserExecutor)
