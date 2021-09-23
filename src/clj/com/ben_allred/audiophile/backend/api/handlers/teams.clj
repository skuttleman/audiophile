(ns com.ben-allred.audiophile.backend.api.handlers.teams
  (:require
    [com.ben-allred.audiophile.backend.api.validations.selectors :as selectors]
    [com.ben-allred.audiophile.backend.domain.interactors.core :as int]
    [com.ben-allred.audiophile.backend.infrastructure.pubsub.core :as ps]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.common.core.utils.maps :as maps]
    [com.ben-allred.audiophile.common.core.utils.uuids :as uuids]))

(defn fetch-all
  "Handles a request to fetch all teams for a user."
  [{:keys [interactor]}]
  (fn [data]
    (int/query-many interactor data)))

(defmethod selectors/select [:get :api/teams]
  [_ request]
  {:user/id (get-in request [:auth/user :user/id])})

(defn fetch
  "Handles a request to fetch one team for a user."
  [{:keys [interactor]}]
  (fn [data]
    (int/query-one interactor data)))

(defmethod selectors/select [:get :api/team]
  [_ request]
  {:user/id (get-in request [:auth/user :user/id])
   :team/id (get-in request [:nav/route :route-params :team-id])})

(defn create
  "Handles a request to create a team."
  [{:keys [pubsub]}]
  (fn [data]
    (let [[opts data] (maps/extract-keys data #{:user/id :request/id})]
      (ps/emit-command! pubsub (:user/id opts) :team/create! data opts))))

(defmethod selectors/select [:post :api/teams]
  [_ request]
  (-> request
      (get-in [:body :data])
      (assoc :user/id (get-in request [:auth/user :user/id]))
      (maps/assoc-maybe :request/id (uuids/->uuid (get-in request [:headers :x-request-id])))))
