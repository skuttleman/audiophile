(ns audiophile.backend.api.repositories.users.queries
  (:require
    [audiophile.backend.api.repositories.core :as repos]
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

(defn find-by-id [executor user-id opts]
  (select! executor (models/select* tbl/users [:= :users.id user-id]) opts))

(defn find-by-email [executor email opts]
  (select! executor (select-by [:= :users.email email]) opts))

(defn insert-user! [executor user _]
  (-> executor
      (select! (models/insert-into tbl/users user))
      :id))

(defn find-by-handle [executor handle opts]
  (select! executor
           (-> [:= :users.handle handle]
               select-by
               (assoc :select [1]))
           opts))

(defn find-by-mobile-number [executor mobile-number opts]
  (select! executor
           (-> [:= :users.mobile-number mobile-number]
               select-by
               (assoc :select [1]))
           opts))
