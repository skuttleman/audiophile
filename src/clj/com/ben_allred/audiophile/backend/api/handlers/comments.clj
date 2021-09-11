(ns com.ben-allred.audiophile.backend.api.handlers.comments
  (:require
    [com.ben-allred.audiophile.backend.domain.interactors.core :as int]
    [com.ben-allred.audiophile.common.core.utils.maps :as maps]))

(defn fetch-all
  "Handles a request to fetch all comments for a file"
  [{:keys [interactor]}]
  (fn [data]
    (int/query-many interactor data)))

(defn create
  "Handles a request to create a comment."
  [{:keys [interactor]}]
  (fn [data]
    (let [[opts data] (maps/extract-keys data #{:user/id :request/id})]
      (int/create! interactor data opts))))
