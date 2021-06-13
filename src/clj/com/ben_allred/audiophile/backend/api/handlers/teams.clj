(ns com.ben-allred.audiophile.backend.api.handlers.teams
  (:require
    [com.ben-allred.audiophile.backend.domain.interactors.core :as int]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]))

(defn fetch-all
  "Handles a request to fetch all teams for a user."
  [{:keys [interactor]}]
  (fn [data]
    (int/query-many interactor data)))

(defn fetch
  "Handles a request to fetch one team for a user."
  [{:keys [interactor]}]
  (fn [data]
    (int/query-one interactor data)))

(defn create
  "Handles a request to create a team."
  [{:keys [interactor]}]
  (fn [project]
    (int/create! interactor project)))
