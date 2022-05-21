(ns audiophile.backend.api.repositories.search.core
  (:require
    [audiophile.backend.api.repositories.search.protocols :as ps]))

(defn find-by-handle
  ([accessor handle]
   (find-by-handle accessor handle nil))
  ([accessor handle opts]
   (ps/find-by-handle accessor handle opts)))

(defn find-by-mobile-number
  ([accessor mobile-number]
   (find-by-mobile-number accessor mobile-number nil))
  ([accessor mobile-number opts]
   (ps/find-by-mobile-number accessor mobile-number opts)))
