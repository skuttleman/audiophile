(ns audiophile.backend.api.handlers.teams
  (:require
    [audiophile.backend.api.validations.selectors :as selectors]
    [audiophile.backend.domain.interactors.core :as int]
    [audiophile.common.core.utils.logger :as log]
    [audiophile.common.core.utils.maps :as maps]
    [audiophile.common.core.utils.uuids :as uuids]))

(defn fetch-all
  "Handles a request to fetch all teams for a user."
  [{:keys [interactor]}]
  (fn [data]
    (int/query-many interactor data)))

(defmethod selectors/select [:get :routes.api/teams]
  [_ request]
  {:user/id   (get-in request [:auth/user :user/id])
   :token/aud (get-in request [:auth/user :jwt/aud])})

(defn fetch
  "Handles a request to fetch one team for a user."
  [{:keys [interactor]}]
  (fn [data]
    (int/query-one interactor data)))

(defmethod selectors/select [:get :routes.api/teams:id]
  [_ request]
  {:user/id   (get-in request [:auth/user :user/id])
   :token/aud (get-in request [:auth/user :jwt/aud])
   :team/id   (get-in request [:nav/route :params :team/id])})

(defn create
  "Handles a request to create a team."
  [{:keys [interactor]}]
  (fn [data]
    (let [[opts data] (maps/extract-keys data #{:user/id :request/id})]
      (int/create! interactor data opts))))

(defmethod selectors/select [:post :routes.api/teams]
  [_ request]
  (-> request
      (get-in [:body :data])
      (assoc :user/id (get-in request [:auth/user :user/id])
             :token/aud (get-in request [:auth/user :jwt/aud]))
      (maps/assoc-maybe :request/id (-> request :headers :x-request-id uuids/->uuid))))

(defn patch
  "Handles a request to patch a team."
  [{:keys [interactor]}]
  (fn [data]
    (let [[opts data] (maps/extract-keys data #{:user/id :request/id})]
      (int/update! interactor data opts))))

(defmethod selectors/select [:patch :routes.api/teams:id]
  [_ request]
  (-> request
      (get-in [:body :data])
      (assoc :team/id (get-in request [:nav/route :params :team/id])
             :user/id (get-in request [:auth/user :user/id])
             :token/aud (get-in request [:auth/user :jwt/aud]))
      (maps/assoc-maybe :request/id (-> request :headers :x-request-id uuids/->uuid))))
