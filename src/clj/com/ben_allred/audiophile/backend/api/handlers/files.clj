(ns com.ben-allred.audiophile.backend.api.handlers.files
  (:require
    [com.ben-allred.audiophile.backend.domain.interactors.core :as int]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]))

(defn upload
  "Handles a request to upload an artifact"
  [{:keys [interactor]}]
  (fn [data]
    (int/create-artifact! interactor data)))

(defn fetch-all
  "Handles a request for all project files."
  [{:keys [interactor]}]
  (fn [data]
    (int/query-many interactor data)))

(defn fetch
  "Handles a request for a single project file."
  [{:keys [interactor]}]
  (fn [data]
    (int/query-one interactor data)))

(defn create
  "Handles a request to create a new file in the system."
  [{:keys [interactor]}]
  (fn [data]
    (int/create-file! interactor data)))

(defn create-version
  "Handles a request to create a new version of an existing file."
  [{:keys [interactor]}]
  (fn [data]
    (int/create-file-version! interactor data)))

(defn download
  "Handles a request to download an uploaded artifact."
  [{:keys [interactor]}]
  (fn [data]
    (int/get-artifact interactor data)))
