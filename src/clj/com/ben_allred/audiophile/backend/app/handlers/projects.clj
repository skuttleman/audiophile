(ns com.ben-allred.audiophile.backend.app.handlers.projects
  (:require
    [com.ben-allred.audiophile.backend.domain.interactors.core :as int]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]))

(defn fetch-all [{:keys [interactor]}]
  (fn [data]
    (int/query-many interactor data)))

(defn fetch [{:keys [interactor]}]
  (fn [data]
    (int/query-one interactor data)))

(defn create [{:keys [interactor]}]
  (fn [project]
    (int/create! interactor project)))
