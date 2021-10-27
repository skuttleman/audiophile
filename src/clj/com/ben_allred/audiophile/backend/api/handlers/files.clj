(ns com.ben-allred.audiophile.backend.api.handlers.files
  (:require
    [clojure.set :as set]
    [com.ben-allred.audiophile.backend.api.validations.selectors :as selectors]
    [com.ben-allred.audiophile.backend.domain.interactors.core :as int]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.common.core.utils.maps :as maps]
    [com.ben-allred.audiophile.common.core.utils.uuids :as uuids]))

(defn upload
  "Handles a request to upload an artifact"
  [{:keys [interactor]}]
  (fn [data]
    (let [[opts data] (maps/extract-keys data #{:user/id :request/id})]
      (int/create-artifact! interactor data opts))))

(defmethod selectors/select [:post :api/artifacts]
  [_ {:keys [headers] :as request}]
  (-> request
      (get-in [:params "files[]"])
      (select-keys #{:filename :content-type :tempfile :size})
      (maps/qualify :artifact)
      (assoc :user/id (get-in request [:auth/user :user/id])
             :token/aud (get-in request [:auth/user :jwt/aud]))
      (maps/assoc-maybe :request/id (uuids/->uuid (:x-request-id headers)))))

(defn fetch-all
  "Handles a request for all project files."
  [{:keys [interactor]}]
  (fn [data]
    (int/query-many interactor data)))

(defmethod selectors/select [:get :api/project.files]
  [_ request]
  {:user/id    (get-in request [:auth/user :user/id])
   :token/aud  (get-in request [:auth/user :jwt/aud])
   :project/id (get-in request [:nav/route :params :project/id])})

(defn fetch
  "Handles a request for a single project file."
  [{:keys [interactor]}]
  (fn [data]
    (int/query-one interactor data)))

(defmethod selectors/select [:get :api/file]
  [_ request]
  {:user/id   (get-in request [:auth/user :user/id])
   :token/aud (get-in request [:auth/user :jwt/aud])
   :file/id   (get-in request [:nav/route :params :file/id])})

(defn create
  "Handles a request to create a new file in the system."
  [{:keys [interactor]}]
  (fn [data]
    (let [[opts data] (maps/extract-keys data #{:user/id :request/id :project/id})]
      (int/create-file! interactor data opts))))

(defmethod selectors/select [:post :api/project.files]
  [_ request]
  (-> request
      (get-in [:body :data])
      (assoc :user/id (get-in request [:auth/user :user/id])
             :token/aud (get-in request [:auth/user :jwt/aud])
             :project/id (get-in request [:nav/route :params :project/id]))
      (maps/assoc-maybe :request/id (uuids/->uuid (get-in request [:headers :x-request-id])))))

(defn create-version
  "Handles a request to create a new version of an existing file."
  [{:keys [interactor]}]
  (fn [data]
    (let [[opts data] (maps/extract-keys data #{:user/id :request/id :file/id})]
      (int/create-file-version! interactor data opts))))

(defmethod selectors/select [:post :api/file]
  [_ request]
  (-> request
      (get-in [:body :data])
      (assoc :user/id (get-in request [:auth/user :user/id])
             :token/aud (get-in request [:auth/user :jwt/aud])
             :file/id (get-in request [:nav/route :params :file/id]))
      (maps/assoc-maybe :request/id (uuids/->uuid (get-in request [:headers :x-request-id])))))

(defn download
  "Handles a request to download an uploaded artifact."
  [{:keys [interactor]}]
  (fn [data]
    (int/get-artifact interactor data)))

(defmethod selectors/select [:get :api/artifact]
  [_ request]
  {:user/id     (get-in request [:auth/user :user/id])
   :token/aud   (get-in request [:auth/user :jwt/aud])
   :artifact/id (get-in request [:nav/route :params :artifact/id])})
