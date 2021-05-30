(ns com.ben-allred.audiophile.api.services.repositories.users.core
  (:refer-clojure :exclude [accessor])
  (:require
    [com.ben-allred.audiophile.api.services.interactors.protocols :as pint]
    [com.ben-allred.audiophile.api.services.repositories.core :as repos]
    [com.ben-allred.audiophile.api.services.repositories.users.queries :as q]
    [com.ben-allred.audiophile.common.utils.logger :as log]))

(deftype UserAccessor [repo]
  pint/IUserAccessor
  pint/IAccessor
  (query-one [_ opts]
    (repos/transact! repo q/find-by-email (:user/email opts) opts)))

(defn accessor [{:keys [repo]}]
  (->UserAccessor repo))
