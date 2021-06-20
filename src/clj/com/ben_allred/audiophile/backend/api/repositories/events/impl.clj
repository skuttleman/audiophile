(ns com.ben-allred.audiophile.backend.api.repositories.events.impl
  (:refer-clojure :exclude [accessor])
  (:require
    [com.ben-allred.audiophile.backend.domain.interactors.protocols :as pint]
    [com.ben-allred.audiophile.backend.api.repositories.events.protocols :as pe]
    [com.ben-allred.audiophile.backend.api.repositories.core :as repos]))

(deftype EventAccessor [repo]
  pint/IEventAccessor
  pint/IAccessor
  (query-many [_ opts]
    (repos/transact! repo pe/select-for-user (:user/id opts) opts)))

(defn accessor
  "Constructor for [[EventAccessor]] which provides semantic access for storing and retrieving events."
  [{:keys [repo]}]
  (->EventAccessor repo))
