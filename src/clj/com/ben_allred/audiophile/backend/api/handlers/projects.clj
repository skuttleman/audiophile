(ns com.ben-allred.audiophile.backend.api.handlers.projects
  (:require
    [com.ben-allred.audiophile.backend.api.validations.selectors :as selectors]
    [com.ben-allred.audiophile.backend.domain.interactors.core :as int]
    [com.ben-allred.audiophile.backend.infrastructure.pubsub.core :as ps]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.common.core.utils.maps :as maps]
    [com.ben-allred.audiophile.common.core.utils.uuids :as uuids]))

(defn fetch-all
  "Handles a request to fetch all projects for a user."
  [{:keys [interactor]}]
  (fn [data]
    (int/query-many interactor data)))

(defmethod selectors/select [:get :api/projects]
  [_ request]
  {:user/id (get-in request [:auth/user :user/id])})

(defn fetch
  "Handles a request to fetch one project for a user."
  [{:keys [interactor]}]
  (fn [data]
    (int/query-one interactor data)))

(defmethod selectors/select [:get :api/project]
  [_ request]
  {:user/id    (get-in request [:auth/user :user/id])
   :project/id (get-in request [:nav/route :route-params :project-id])})

(defn create
  "Handles a request to create a project."
  [{:keys [pubsub]}]
  (fn [data]
    (let [[opts data] (maps/extract-keys data #{:user/id :request/id})]
      (ps/emit-command! pubsub (:user/id opts) :project/create! data opts))))

(defmethod selectors/select [:post :api/projects]
  [_ request]
  (-> request
      (get-in [:body :data])
      (assoc :user/id (get-in request [:auth/user :user/id]))
      (maps/assoc-maybe :request/id (uuids/->uuid (get-in request [:headers :x-request-id])))))
