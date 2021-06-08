(ns com.ben-allred.audiophile.backend.infrastructure.db.users
  (:require
    [com.ben-allred.audiophile.backend.infrastructure.db.models.core :as models]
    [com.ben-allred.audiophile.backend.api.repositories.users.protocols :as pu]
    [com.ben-allred.audiophile.backend.api.repositories.core :as repos]
    [com.ben-allred.audiophile.common.core.utils.colls :as colls]))

(defn ^:private select-by [model clause]
  (-> model
      (models/remove-fields #{:mobile-number})
      (models/select* clause)))

(deftype UserExecutor [executor users user-teams]
  pu/IUserExecutor
  (find-by-email [_ email opts]
    (colls/only! (repos/execute! executor
                                 (select-by users [:= :users.email email])
                                 opts))))

(defn ->executor [{:keys [users user-teams]}]
  (fn [executor]
    (->UserExecutor executor users user-teams)))
