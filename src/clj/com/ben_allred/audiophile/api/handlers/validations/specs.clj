(ns com.ben-allred.audiophile.api.handlers.validations.specs
  (:require
    [clojure.spec.alpha :as s]
    [clojure.string :as string]
    [com.ben-allred.audiophile.common.utils.colls :as colls]
    [com.ben-allred.audiophile.common.utils.logger :as log]
    [com.ben-allred.audiophile.common.utils.uuids :as uuids])
  (:import
    (java.io File)))

;; common
(s/def ::trimmed-string (s/and string?
                               #(= % (not-empty (string/trim %)))))
(s/def :user/id uuid?)

;; projects
(s/def :project/id uuid?)
(s/def :project/name ::trimmed-string)
(s/def :project/team-id uuid?)

;; teams
(s/def :team/id uuid?)
(s/def :team/name ::trimmed-string)
(s/def :team/type #{:COLLABORATIVE})

;; files
(s/def :artifact/content-type ::trimmed-string)
(s/def :artifact/filename ::trimmed-string)
(s/def :artifact/id uuid?)
(s/def :artifact/size nat-int?)
(s/def :artifact/tempfile #(instance? File %))
(s/def :file/name ::trimmed-string)
(s/def :version/name ::trimmed-string)

;; API
(s/def :api.common/auth
  (s/keys :req [:user/id]))
(s/def :api.common/file-id
  (s/merge :api.common/auth
           (s/keys :req [:file/id])))
(s/def :api.common/project-id
  (s/merge :api.common/auth
           (s/keys :req [:project/id])))

(s/def :api.artifact/create
  (s/merge :api.common/auth
           (s/keys :req-un [:artifact/filename :artifact/content-type :artifact/tempfile :artifact/size])))
(s/def :api.file/create
  (s/merge :api.common/project-id
           (s/keys :req [:file/name :artifact/id :version/name])))
(s/def :api.project/create
  (s/merge :api.common/auth
           (s/keys :req [:project/name :project/team-id])))
(s/def :api.team/create
  (s/merge :api.common/auth
           (s/keys :req [:team/name :team/type])))
(s/def :api.version/create
  (s/merge :api.common/file-id
           (s/keys :req [:artifact/id :version/name])))

(defmulti spec
          "returns a spec and conformed data for a validating a request for a route. defaults to `nil`."
          (fn [handler _] handler))
(defmethod spec :default
  [_ request]
  request)

(defmethod spec [:get :api/project.files]
  [_ request]
  {:user/id    (get-in request [:auth/user :user/id])
   :project/id (get-in request [:nav/route :route-params :project-id])})

(defmethod spec [:get :api/project]
  [_ request]
  {:user/id    (get-in request [:auth/user :user/id])
   :project/id (get-in request [:nav/route :route-params :project-id])})

(defmethod spec [:get :api/projects]
  [_ request]
  {:user/id (get-in request [:auth/user :user/id])})

(defmethod spec [:get :api/team]
  [_ request]
  {:user/id (get-in request [:auth/user :user/id])
   :team/id (get-in request [:nav/route :route-params :team-id])})

(defmethod spec [:get :api/teams]
  [_ request]
  {:user/id (get-in request [:auth/user :user/id])})

(defmethod spec [:post :api/artifacts]
  [_ request]
  (-> request
      (get-in [:params "files[]"])
      (assoc :user/id (get-in request [:auth/user :user/id]))))

(defmethod spec [:post :api/project.file]
  [_ request]
  (-> request
      (get-in [:body :data])
      (assoc :user/id (get-in request [:auth/user :user/id]))
      (assoc :file/id (get-in request [:nav/route :route-params :file-id]))))

(defmethod spec [:post :api/project.files]
  [_ request]
  (-> request
      (get-in [:body :data])
      (assoc :user/id (get-in request [:auth/user :user/id]))
      (assoc :project/id (get-in request [:nav/route :route-params :project-id]))))

(defmethod spec [:post :api/projects]
  [_ request]
  (-> request
      (get-in [:body :data])
      (assoc :user/id (get-in request [:auth/user :user/id]))))

(defmethod spec [:post :api/teams]
  [_ request]
  (-> request
      (get-in [:body :data])
      (assoc :user/id (get-in request [:auth/user :user/id]))))
