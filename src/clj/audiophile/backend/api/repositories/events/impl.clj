(ns audiophile.backend.api.repositories.events.impl
  (:refer-clojure :exclude [accessor])
  (:require
    [audiophile.backend.api.repositories.events.queries :as q]
    [audiophile.backend.api.repositories.core :as repos]
    [audiophile.backend.domain.interactors.protocols :as pint]))

(deftype EventAccessor [repo]
  pint/IEventAccessor
  pint/IAccessor
  (query-many [_ opts]
    (repos/transact! repo q/select-for-user (:user/id opts) opts)))

(defn accessor
  "Constructor for [[EventAccessor]] which provides semantic access for storing and retrieving events."
  [{:keys [repo]}]
  (->EventAccessor repo))
