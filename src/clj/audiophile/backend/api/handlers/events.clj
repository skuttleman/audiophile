(ns audiophile.backend.api.handlers.events
  (:require
    [audiophile.backend.api.validations.selectors :as selectors]
    [audiophile.backend.domain.interactors.core :as int]
    [audiophile.common.core.utils.maps :as maps]
    [audiophile.common.core.utils.uuids :as uuids]))

(defn fetch-all
  "Handles a request to fetch events for a user."
  [{:keys [interactor]}]
  (fn [data]
    (int/query-many interactor data)))

(defmethod selectors/select [:get :api/events]
  [_ request]
  (-> {:user/id   (get-in request [:auth/user :user/id])
       :token/aud (get-in request [:auth/user :jwt/aud])}
      (maps/assoc-maybe :filter/since (some-> request
                                              (get-in [:nav/route :params :since])
                                              uuids/->uuid))))
