(ns com.ben-allred.audiophile.backend.api.repositories.users.impl
  (:refer-clojure :exclude [accessor])
  (:require
    [com.ben-allred.audiophile.backend.api.pubsub.core :as ps]
    [com.ben-allred.audiophile.backend.api.repositories.core :as repos]
    [com.ben-allred.audiophile.backend.api.repositories.users.core :as users]
    [com.ben-allred.audiophile.backend.domain.interactors.protocols :as pint]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]))

(defmulti ^:private find-by (fn [_ {:user/keys [email id]}]
                              (cond
                                id :user/id
                                email :user/email)))

(defmethod find-by :user/id
  [executor opts]
  (users/find-by-id executor (:user/id opts) opts))

(defmethod find-by :user/email
  [executor opts]
  (users/find-by-email executor (:user/email opts) opts))

(deftype UserAccessor [repo ch]
  pint/IUserAccessor
  pint/IAccessor
  (query-one [_ opts]
    (repos/transact! repo find-by opts))
  (create! [_ data opts]
    (ps/emit-command! ch :user/create! data opts)))

(defn accessor
  "Constructor for [[UserAccessor]] which provides semantic access for storing and retrieving users."
  [{:keys [ch repo]}]
  (->UserAccessor repo ch))
