(ns com.ben-allred.audiophile.backend.infrastructure.db.users
  (:require
    [com.ben-allred.audiophile.backend.api.repositories.core :as repos]
    [com.ben-allred.audiophile.backend.api.repositories.search.protocols :as ps]
    [com.ben-allred.audiophile.backend.api.repositories.users.protocols :as pu]
    [com.ben-allred.audiophile.backend.infrastructure.db.models.core :as models]
    [com.ben-allred.audiophile.common.core.utils.colls :as colls]))

(defn ^:private select-by [model clause]
  (-> model
      (models/select-fields #{:id :first-name :handle})
      (models/select* clause)))

(deftype UserExecutor [executor users user-teams]
  pu/IUserExecutor
  (find-by-email [_ email opts]
    (colls/only! (repos/execute! executor
                                 (select-by users [:= :users.email email])
                                 opts)))
  (insert-user! [_ user _]
    (-> executor
        (repos/execute! (models/insert-into users user))
        colls/only!
        :id))

  ps/ISearchUserExecutor
  (find-by-handle [_ handle opts]
    (colls/only! (repos/execute! executor
                                 (-> users
                                     (select-by [:= :users.handle handle])
                                     (assoc :select [1]))
                                 opts)))
  (find-by-mobile-number [_ mobile-number opts]
    (colls/only! (repos/execute! executor
                                 (-> users
                                     (select-by [:= :users.mobile-number mobile-number])
                                     (assoc :select [1]))
                                 opts))))

(defn ->executor
  "Factory function for creating [[UserExecutor]] which provides access to the user repository
   inside a transaction."
  [{:keys [users user-teams]}]
  (fn [executor]
    (->UserExecutor executor users user-teams)))
