(ns com.ben-allred.audiophile.api.services.repositories.users
  (:require
    [com.ben-allred.audiophile.api.services.repositories.core :as repos]))

(defn ^:private query* [clause]
  {:select [:users.*]
   :from   [:users]
   :where  clause})

(defn query-by-email [transactor email]
  (first (repos/transact! transactor repos/execute! (query* [:= :users.email email]))))
