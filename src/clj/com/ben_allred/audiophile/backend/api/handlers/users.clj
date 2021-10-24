(ns com.ben-allred.audiophile.backend.api.handlers.users
  (:require
    [com.ben-allred.audiophile.backend.api.validations.selectors :as selectors]
    [com.ben-allred.audiophile.backend.domain.interactors.core :as int]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.common.core.utils.maps :as maps]
    [com.ben-allred.audiophile.common.core.utils.uuids :as uuids]))

(defn create
  "Handles a request to create a team."
  [{:keys [interactor]}]
  (fn [data]
    (let [[opts data] (maps/extract-keys data #{:request/id :user/id})]
      (int/create! interactor data opts))))

(defmethod selectors/select [:post :api/users]
  [_ {user :auth/user :as request}]
  (when (get-in user [:jwt/aud :token/signup])
    (-> request
        (get-in [:body :data])
        (merge user)
        (maps/assoc-maybe :request/id (uuids/->uuid (get-in request [:headers :x-request-id]))))))