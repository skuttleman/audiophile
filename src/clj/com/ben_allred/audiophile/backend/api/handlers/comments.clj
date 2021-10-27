(ns com.ben-allred.audiophile.backend.api.handlers.comments
  (:require
    [com.ben-allred.audiophile.backend.api.validations.selectors :as selectors]
    [com.ben-allred.audiophile.backend.domain.interactors.core :as int]
    [com.ben-allred.audiophile.common.core.utils.maps :as maps]
    [com.ben-allred.audiophile.common.core.utils.uuids :as uuids]))

(defn fetch-all
  "Handles a request to fetch all comments for a file"
  [{:keys [interactor]}]
  (fn [data]
    (int/query-many interactor data)))

(defmethod selectors/select [:get :api/file.comments]
  [_ request]
  {:user/id   (get-in request [:auth/user :user/id])
   :token/aud (get-in request [:auth/user :jwt/aud])
   :file/id   (get-in request [:nav/route :params :file/id])})

(defn create
  "Handles a request to create a comment."
  [{:keys [interactor]}]
  (fn [data]
    (let [[opts data] (maps/extract-keys data #{:user/id :request/id})]
      (int/create! interactor data opts))))

(defmethod selectors/select [:post :api/comments]
  [_ request]
  (-> request
      (get-in [:body :data])
      (assoc :user/id (get-in request [:auth/user :user/id])
             :token/aud (get-in request [:auth/user :jwt/aud]))
      (maps/assoc-maybe :request/id (uuids/->uuid (get-in request [:headers :x-request-id])))))
