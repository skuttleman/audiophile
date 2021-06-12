(ns com.ben-allred.audiophile.backend.api.validations.selectors
  (:require
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.common.core.utils.maps :as maps]
    [com.ben-allred.audiophile.common.core.utils.uuids :as uuids]))

(defmulti select
          "extracts input data from"
          (fn [handler _] handler))

(defmethod select :default
  [_ request]
  request)

(defmethod select [:get :api/file]
  [_ request]
  {:user/id (get-in request [:auth/user :user/id])
   :file/id (get-in request [:nav/route :route-params :file-id])})

(defmethod select [:get :api/project.files]
  [_ request]
  {:user/id    (get-in request [:auth/user :user/id])
   :project/id (get-in request [:nav/route :route-params :project-id])})

(defmethod select [:get :api/project]
  [_ request]
  {:user/id    (get-in request [:auth/user :user/id])
   :project/id (get-in request [:nav/route :route-params :project-id])})

(defmethod select [:get :api/projects]
  [_ request]
  {:user/id (get-in request [:auth/user :user/id])})

(defmethod select [:get :api/team]
  [_ request]
  {:user/id (get-in request [:auth/user :user/id])
   :team/id (get-in request [:nav/route :route-params :team-id])})

(defmethod select [:get :api/teams]
  [_ request]
  {:user/id (get-in request [:auth/user :user/id])})

(defmethod select [:get :ws/connection]
  [_ request]
  (-> request
      (assoc :user/id (get-in request [:auth/user :user/id]))
      (merge (:headers request) (get-in request [:nav/route :query-params]))))

(defmethod select [:post :api/artifacts]
  [_ request]
  (-> request
      (get-in [:params "files[]"])
      (assoc :user/id (get-in request [:auth/user :user/id]))
      (maps/assoc-maybe :request/id (uuids/->uuid (get-in request [:headers :x-request-id])))))

(defmethod select [:post :api/file]
  [_ request]
  (-> request
      (get-in [:body :data])
      (assoc :user/id (get-in request [:auth/user :user/id]))
      (assoc :file/id (get-in request [:nav/route :route-params :file-id]))))

(defmethod select [:post :api/project.files]
  [_ request]
  (-> request
      (get-in [:body :data])
      (assoc :user/id (get-in request [:auth/user :user/id]))
      (assoc :project/id (get-in request [:nav/route :route-params :project-id]))))

(defmethod select [:post :api/projects]
  [_ request]
  (-> request
      (get-in [:body :data])
      (assoc :user/id (get-in request [:auth/user :user/id]))))

(defmethod select [:post :api/teams]
  [_ request]
  (-> request
      (get-in [:body :data])
      (assoc :user/id (get-in request [:auth/user :user/id]))))

(defmethod select [:get :api/artifact]
  [_ request]
  {:user/id     (get-in request [:auth/user :user/id])
   :artifact/id (get-in request [:nav/route :route-params :artifact-id])})
