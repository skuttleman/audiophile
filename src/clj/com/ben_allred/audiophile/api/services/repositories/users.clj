(ns com.ben-allred.audiophile.api.services.repositories.users
  (:require
    [com.ben-allred.audiophile.api.services.repositories.core :as repos]
    [com.ben-allred.audiophile.api.services.repositories.entities.core :as entities]
    [com.ben-allred.audiophile.common.utils.fns :as fns]
    [com.ben-allred.audiophile.common.utils.logger :as log]))

(defn query-by-email [repo email]
  (-> repo
      (repos/transact! repos/->exec!
                       repos/execute!
                       (fns/=> :entity/users
                               (update :fields (partial remove #{:mobile-number}))
                               (assoc :where [:= :users.email email])
                               entities/select*))
      first))
