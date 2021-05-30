(ns com.ben-allred.audiophile.api.services.repositories.users.queries
  (:require
    [com.ben-allred.audiophile.api.services.repositories.models.core :as models]
    [com.ben-allred.audiophile.api.services.repositories.users.protocols :as pu]
    [com.ben-allred.audiophile.api.services.repositories.core :as repos]
    [com.ben-allred.audiophile.common.utils.colls :as colls]))

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

(defn find-by-email
  ([executor email]
   (find-by-email executor email nil))
  ([executor email opts]
   (pu/find-by-email executor email opts)))
