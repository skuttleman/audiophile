(ns com.ben-allred.audiophile.backend.api.handlers.files
  (:require
    [com.ben-allred.audiophile.backend.domain.interactors.core :as int]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]))

(defn upload [{:keys [interactor]}]
  (fn [data]
    (int/create-artifact! interactor data)))

(defn fetch-all [{:keys [interactor]}]
  (fn [data]
    (int/query-many interactor data)))

(defn fetch [{:keys [interactor]}]
  (fn [data]
    (int/query-one interactor data)))

(defn create [{:keys [interactor]}]
  (fn [data]
    (int/create-file! interactor data)))

(defn create-version [{:keys [interactor]}]
  (fn [data]
    (int/create-file-version! interactor data)))

(defn download [{:keys [interactor]}]
  (fn [data]
    (int/get-artifact interactor data)))
