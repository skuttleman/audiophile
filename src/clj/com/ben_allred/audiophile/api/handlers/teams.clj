(ns com.ben-allred.audiophile.api.handlers.teams
  (:require
    [com.ben-allred.audiophile.api.services.interactors.core :as int]
    [com.ben-allred.audiophile.common.utils.logger :as log]))

(defn fetch-all [{:keys [interactor]}]
  (fn [data]
    (int/query-many interactor data)))

(defn fetch [{:keys [interactor]}]
  (fn [data]
    (int/query-one interactor data)))

(defn create [{:keys [interactor]}]
  (fn [data]
    (int/create! interactor data)))
