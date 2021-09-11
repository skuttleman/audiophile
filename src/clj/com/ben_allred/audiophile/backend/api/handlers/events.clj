(ns com.ben-allred.audiophile.backend.api.handlers.events
  (:require
    [com.ben-allred.audiophile.backend.api.validations.selectors :as selectors]
    [com.ben-allred.audiophile.backend.domain.interactors.core :as int]
    [com.ben-allred.audiophile.common.core.utils.maps :as maps]
    [com.ben-allred.audiophile.common.core.utils.uuids :as uuids]))

(defn fetch-all
  "Handles a request to fetch events for a user."
  [{:keys [interactor]}]
  (fn [data]
    (int/query-many interactor data)))

(defmethod selectors/select [:get :api/events]
  [_ request]
  (-> {:user/id (get-in request [:auth/user :user/id])}
      (maps/assoc-maybe :filter/since (some-> request
                                              (get-in [:nav/route :query-params :since])
                                              uuids/->uuid))))
