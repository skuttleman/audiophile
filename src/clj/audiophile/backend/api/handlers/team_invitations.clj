(ns audiophile.backend.api.handlers.team-invitations
  (:require
    [audiophile.backend.api.validations.selectors :as selectors]
    [audiophile.backend.domain.interactors.core :as int]
    [audiophile.common.core.utils.maps :as maps]
    [audiophile.common.core.utils.uuids :as uuids]))

(defn fetch-all
  "Queries the user's invitations"
  [{:keys [interactor]}]
  (fn [data]
    (int/query-many interactor data)))

(defmethod selectors/select [:get :routes.api/team-invitations]
  [_ {user :auth/user}]
  (-> user
      (select-keys #{:user/id})
      (assoc :token/aud (:jwt/aud user))))

(defn create
  "Handles a request to invite a new team member"
  [{:keys [interactor]}]
  (fn [data]
    (let [[opts data] (maps/extract-keys data #{:user/id :request/id})]
      (int/create! interactor data opts))))

(defmethod selectors/select [:post :routes.api/team-invitations]
  [_ request]
  (-> request
      (get-in [:body :data])
      (assoc :user/id (get-in request [:auth/user :user/id])
             :token/aud (get-in request [:auth/user :jwt/aud]))
      (maps/assoc-maybe :request/id (-> request :headers :x-request-id uuids/->uuid))))

(defn modify
  "Handles a request to update the status of an invitation"
  [{:keys [interactor]}]
  (fn [data]
    (let [[opts data] (maps/extract-keys data #{:user/id :request/id})]
      (int/update! interactor data opts))))

(defmethod selectors/select [:patch :routes.api/team-invitations]
  [_ request]
  (-> request
      (get-in [:body :data])
      (assoc :user/id (get-in request [:auth/user :user/id])
             :token/aud (get-in request [:auth/user :jwt/aud]))
      (maps/assoc-maybe :request/id (-> request :headers :x-request-id uuids/->uuid))))
