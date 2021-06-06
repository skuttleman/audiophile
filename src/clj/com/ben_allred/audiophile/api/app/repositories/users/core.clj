(ns com.ben-allred.audiophile.api.app.repositories.users.core
  (:refer-clojure :exclude [accessor])
  (:require
    [com.ben-allred.audiophile.api.domain.interactors.protocols :as pint]
    [com.ben-allred.audiophile.api.app.repositories.core :as repos]
    [com.ben-allred.audiophile.api.app.repositories.users.protocols :as pu]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]))

(deftype UserAccessor [repo]
  pint/IUserAccessor
  pint/IAccessor
  (query-one [_ opts]
    (repos/transact! repo pu/find-by-email (:user/email opts) opts)))

(defn accessor [{:keys [repo]}]
  (->UserAccessor repo))
