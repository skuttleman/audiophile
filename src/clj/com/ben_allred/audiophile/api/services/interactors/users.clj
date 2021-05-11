(ns com.ben-allred.audiophile.api.services.interactors.users
  (:require
    [com.ben-allred.audiophile.api.services.repositories.core :as repos]
    [com.ben-allred.audiophile.api.services.repositories.users :as users]
    [com.ben-allred.audiophile.common.utils.colls :as colls]
    [com.ben-allred.audiophile.common.utils.fns :as fns]
    [com.ben-allred.audiophile.common.utils.logger :as log]))

(defn query-by-email
  "Find a user by email address."
  [repo email]
  (-> repo
      (repos/transact! repos/->exec!
                       (fns/=> :entity/users
                               (users/select-by [:= :users.email email])))
      colls/only!))
