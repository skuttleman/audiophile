(ns com.ben-allred.audiophile.backend.api.handlers.events
  (:require
    [com.ben-allred.audiophile.backend.domain.interactors.core :as int]))

(defn fetch-all
  "Handles a request to fetch events for a user."
  [{:keys [interactor]}]
  (fn [data]
    (int/query-many interactor data)))
