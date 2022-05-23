(ns audiophile.backend.api.handlers.users
  (:require
    [audiophile.backend.api.validations.selectors :as selectors]
    [audiophile.backend.domain.interactors.core :as int]
    [audiophile.common.core.utils.logger :as log]
    [audiophile.common.core.utils.maps :as maps]
    [audiophile.common.core.utils.uuids :as uuids]))

(defn create
  "Handles a request to create a team."
  [{:keys [interactor]}]
  (fn [data]
    (let [[opts data] (maps/extract-keys data #{:request/id :user/id})]
      (int/create! interactor data opts))))

(defmethod selectors/select [:post :api/users]
  [_ {user :auth/user :as request}]
  (-> request
      (get-in [:body :data])
      (merge user)
      (assoc :token/aud (:jwt/aud user))
      (maps/assoc-maybe :request/id (uuids/->uuid (get-in request [:headers :x-request-id])))))

(defn profile
  "Loads the user's profile data"
  [{:keys [interactor]}]
  (fn [data]
    (int/query-one interactor data)))

(defmethod selectors/select [:get :api/profile]
  [_ {user :auth/user}]
  (-> user
      (select-keys #{:user/id :user/email})
      (assoc :token/aud (:jwt/aud user)
             :auth/user user)))
