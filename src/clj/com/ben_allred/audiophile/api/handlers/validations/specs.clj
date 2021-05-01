(ns com.ben-allred.audiophile.api.handlers.validations.specs
  (:require
    [clojure.spec.alpha :as s]
    [clojure.string :as string]))

;; common
(s/def ::trimmed-string (s/and string?
                               #(= % (not-empty (string/trim %)))))

;; projects
(s/def :project/name ::trimmed-string)
(s/def :project/team-id uuid?)
(s/def ::project-new
  (s/keys :req [:project/name
                :project/team-id]))

;; teams
(s/def :team/name ::trimmed-string)
(s/def :team/type #{:COLLABORATIVE})
(s/def ::team-new
  (s/keys :req [:team/name
                :team/type]))

(defmulti spec
          "returns a spec for a validating a request for a route. defaults to `nil`."
          identity)
(defmethod spec :default
  [_])

(defmethod spec [:post :api/projects]
  [_]
  ::project-new)

(defmethod spec [:post :api/teams]
  [_]
  ::team-new)
