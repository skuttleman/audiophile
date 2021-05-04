(ns com.ben-allred.audiophile.api.handlers.validations.specs
  (:require
    [clojure.spec.alpha :as s]
    [clojure.string :as string]
    [com.ben-allred.audiophile.common.utils.uuids :as uuids])
  (:import
    (java.io File)))

;; common
(s/def ::trimmed-string (s/and string?
                               #(= % (not-empty (string/trim %)))))

;; projects
(s/def :project/name ::trimmed-string)
(s/def :project/team-id uuid?)

(s/def ::project-new
  (s/keys :req [:project/name :project/team-id]))

;; teams
(s/def :team/name ::trimmed-string)
(s/def :team/type #{:COLLABORATIVE})

(s/def ::team-new
  (s/keys :req [:team/name :team/type]))

;; files
(s/def :artifact/id uuid?)
(s/def :artifact/filename ::trimmed-string)
(s/def :artifact/content-type ::trimmed-string)
(s/def :artifact/tempfile #(instance? File %))
(s/def :artifact/size nat-int?)
(s/def :artifact/params
  (s/keys :req-un [:artifact/filename :artifact/content-type :artifact/tempfile :artifact/size]))

(s/def :files/project-id
  (s/conformer (fn [s]
                 (or (try (uuids/->uuid s)
                          (catch Throwable _))
                     ::s/invalid))))
(s/def :file/name ::trimmed-string)

(s/def :version/name ::trimmed-string)

(s/def ::artifact-new
  (s/coll-of :artifact/params))
(s/def ::file-version-new
  (s/keys :req [:artifact/id :version/name]))
(s/def ::file-new
  (s/keys :req [:file/name :artifact/id :version/name]))

(defmulti spec
          "returns a spec and conformed data for a validating a request for a route. defaults to `nil`."
          identity)
(defmethod spec :default
  [_])

(defmethod spec [:post :api/project.file]
  [request]
  [::file-version-new (get-in request [:body :data])])

(defmethod spec [:post :api/project.files]
  [request]
  [::file-new (get-in request [:body :data])])

(defmethod spec [:post :api/artifacts]
  [request]
  [::artifact-new (get-in request [:params "files[]"])])

(defmethod spec [:post :api/projects]
  [request]
  [::project-new (get-in request [:body :data])])

(defmethod spec [:post :api/teams]
  [request]
  [::team-new (get-in request [:body :data])])
