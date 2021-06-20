(ns com.ben-allred.audiophile.backend.api.handlers.files
  (:require
    [com.ben-allred.audiophile.backend.domain.interactors.core :as int]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.common.core.utils.maps :as maps]))

(defn upload
  "Handles a request to upload an artifact"
  [{:keys [interactor]}]
  (fn [data]
    (let [[opts data] (maps/extract-keys data #{:user/id :request/id})]
      (int/create-artifact! interactor data opts))))

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
    (let [[opts data] (maps/extract-keys data #{:user/id :request/id :project/id})]
      (int/create-file! interactor data opts))))

(defn create-version
  "Handles a request to create a new version of an existing file."
  [{:keys [interactor]}]
  (fn [data]
    (let [[opts data] (maps/extract-keys data #{:user/id :request/id :file/id})]
      (int/create-file-version! interactor data opts))))

(defn download
  "Handles a request to download an uploaded artifact."
  [{:keys [interactor]}]
  (fn [data]
    (int/get-artifact interactor data)))
