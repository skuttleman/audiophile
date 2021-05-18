(ns com.ben-allred.audiophile.api.services.repositories.users.core
  (:require
    [com.ben-allred.audiophile.api.services.interactors.protocols :as pint]
    [com.ben-allred.audiophile.api.services.repositories.core :as repos]
    [com.ben-allred.audiophile.api.services.repositories.users.queries :as q]
    [com.ben-allred.audiophile.common.utils.colls :as colls]
    [com.ben-allred.audiophile.common.utils.logger :as log]
    [integrant.core :as ig]))

(defn ^:private query-by-email* [{model :models/users} email]
  (q/select-by model [:= :users.email email]))

(deftype UserAccessor [repo]
  pint/IAccessor
  (query-one [_ opts]
    (colls/only! (repos/transact! repo repos/->exec! query-by-email* (:user/email opts)))))

(defmethod ig/init-key ::model [_ {:keys [repo]}]
  (->UserAccessor repo))
