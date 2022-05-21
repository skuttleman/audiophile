(ns audiophile.backend.api.repositories.search.impl
  (:refer-clojure :exclude [accessor])
  (:require
    [audiophile.backend.api.repositories.core :as repos]
    [audiophile.backend.api.repositories.search.core :as search]
    [audiophile.backend.domain.interactors.protocols :as pint]))

(deftype SearchAccessor [users]
  pint/ISearchAccessor
  (exists? [_ {:search/keys [field value] :as opts}]
    (boolean (case field
               :user/handle
               (repos/transact! users search/find-by-handle value opts)

               :user/mobile-number
               (repos/transact! users search/find-by-mobile-number value opts)))))

(defn accessor
  "Constructor for [[SearchAccessor]] which provides semantic access for searching for unique values in use."
  [{:keys [users]}]
  (->SearchAccessor users))
