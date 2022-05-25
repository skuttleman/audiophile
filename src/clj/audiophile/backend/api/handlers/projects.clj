(ns audiophile.backend.api.handlers.projects
  (:require
    [audiophile.backend.api.validations.selectors :as selectors]
    [audiophile.backend.domain.interactors.core :as int]
    [audiophile.common.core.utils.logger :as log]
    [audiophile.common.core.utils.maps :as maps]
    [audiophile.common.core.utils.uuids :as uuids]))

(defn fetch-all
  "Handles a request to fetch all projects for a user."
  [{:keys [interactor]}]
  (fn [data]
    (int/query-many interactor data)))

(defmethod selectors/select [:get :routes.api/projects]
  [_ request]
  {:user/id   (get-in request [:auth/user :user/id])
   :token/aud (get-in request [:auth/user :jwt/aud])})

(defn fetch
  "Handles a request to fetch one project for a user."
  [{:keys [interactor]}]
  (fn [data]
    (int/query-one interactor data)))

(defmethod selectors/select [:get :routes.api/projects:id]
  [_ request]
  {:user/id    (get-in request [:auth/user :user/id])
   :token/aud  (get-in request [:auth/user :jwt/aud])
   :project/id (get-in request [:nav/route :params :project/id])})

(defn create
  "Handles a request to create a project."
  [{:keys [interactor]}]
  (fn [data]
    (let [[opts data] (maps/extract-keys data #{:user/id :request/id})]
      (int/create! interactor data opts))))

(defmethod selectors/select [:post :routes.api/projects]
  [_ request]
  (-> request
      (get-in [:body :data])
      (assoc :user/id (get-in request [:auth/user :user/id])
             :token/aud (get-in request [:auth/user :jwt/aud]))
      (maps/assoc-maybe :request/id (uuids/->uuid (get-in request [:headers :x-request-id])))))
