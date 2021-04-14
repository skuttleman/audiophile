(ns com.ben-allred.audiophile.api.services.repositories.users
  (:require
    [com.ben-allred.audiophile.api.services.repositories.core :as repos]))

(defn ^:private query* [clause]
  {:select [[:users.id "user/id"]
            [:users.first-name "user/first-name"]
            [:users.last-name "user/last-name"]
            [:users.handle "user/handle"]
            [:users.email "user/email"]
            [:users.created-at "user/created-at"]]
   :from   [:users]
   :where  clause})

(defn query-by-email [transactor email]
  (first (repos/transact! transactor repos/execute! (query* [:= :users.email email]))))
