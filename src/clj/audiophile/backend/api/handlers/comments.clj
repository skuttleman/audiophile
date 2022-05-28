(ns audiophile.backend.api.handlers.comments
  (:require
    [audiophile.backend.api.validations.selectors :as selectors]
    [audiophile.backend.domain.interactors.core :as int]
    [audiophile.common.core.utils.logger :as log]
    [audiophile.common.core.utils.maps :as maps]
    [audiophile.common.core.utils.uuids :as uuids]))

(defn fetch-all
  "Handles a request to fetch all comments for a file"
  [{:keys [interactor]}]
  (fn [data]
    (int/query-many interactor data)))

(defmethod selectors/select [:get :routes.api/files:id.comments]
  [_ request]
  {:user/id         (get-in request [:auth/user :user/id])
   :token/aud       (get-in request [:auth/user :jwt/aud])
   :file/id         (get-in request [:nav/route :params :file/id])
   :file-version/id (uuids/->uuid (get-in request [:nav/route :params :file-version-id]))})

(defn create
  "Handles a request to create a comment."
  [{:keys [interactor]}]
  (fn [data]
    (let [[opts data] (maps/extract-keys data #{:user/id :request/id})]
      (int/create! interactor data opts))))

(defmethod selectors/select [:post :routes.api/comments]
  [_ request]
  (let [{user-id :user/id aud :jwt/aud} (:auth/user request)]
    (-> request
        (get-in [:body :data])
        (assoc :user/id user-id :token/aud aud)
        (maps/assoc-maybe :request/id (uuids/->uuid (get-in request [:headers :x-request-id]))))))
