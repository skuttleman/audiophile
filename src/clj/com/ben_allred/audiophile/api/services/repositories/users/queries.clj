(ns com.ben-allred.audiophile.api.services.repositories.users.queries
  (:require
    [com.ben-allred.audiophile.api.services.repositories.entities.core :as entities]))

(defn select-by [entity clause]
  (-> entity
      (update :fields (partial remove #{:mobile-number}))
      (assoc :where clause)
      entities/select*))
