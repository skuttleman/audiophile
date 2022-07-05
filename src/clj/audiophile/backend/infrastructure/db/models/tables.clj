(ns audiophile.backend.infrastructure.db.models.tables
  (:require
    [audiophile.common.core.utils.maps :as maps]))

(def artifacts
  {:fields    #{:key :filename :content-length :id :content-type :uri :created-at}
   :spec      [:map
               [:artifact/id uuid?]
               [:artifact/content-length integer?]
               [:artifact/created-at inst?]
               [:artifact/key string?]
               [:artifact/uri string?]
               [:artifact/filename string?]
               [:artifact/content-type string?]]
   :table     :artifacts
   :namespace :artifact})

(def comments
  {:fields    #{:id :comment-id :body :selection :created-at :file-version-id :created-by}
   :spec      [:map
               [:comment/id uuid?]
               [:comment/file-version-id uuid?]
               [:comment/selection {:optional true} [:tuple number? number?]]
               [:comment/comment-id {:optional true} uuid?]
               [:comment/created-at inst?]
               [:comment/body string?]]
   :casts     {:selection :numrange}
   :table     :comments
   :namespace :comment})

(def event-types
  {:fields    #{:category :name :id}
   :spec      [:map
               [:event-type/id uuid?]
               [:event-type/category string?]
               [:event-type/name string?]]
   :table     :event-types
   :namespace :event-type})

(def events
  {:fields    #{:model-id :emitted-by :emitted-at :event-type-id :id :ctx :data}
   :spec      [:map
               [:event/data {:optional true} any?]
               [:event/id uuid?]
               [:event/event-type-id uuid?]
               [:event/model-id uuid?]
               [:event/emitted-by {:optional true} uuid?]
               [:event/emitted-at inst?]
               [:event/ctx {:optional true} any?]]
   :casts     {:ctx :jsonb :data :custom/edn}
   :table     :events
   :namespace :event})

(def file-versions
  {:fields    #{:file-id :artifact-id :name :id :created-at}
   :spec      [:map
               [:file-version/id uuid?]
               [:file-version/artifact-id uuid?]
               [:file-version/file-id uuid?]
               [:file-version/created-at inst?]
               [:file-version/name string?]]
   :table     :file-versions
   :namespace :file-version})

(def files
  {:fields    #{:name :id :idx :project-id :created-at}
   :spec      [:map
               [:file/id uuid?]
               [:file/project-id uuid?]
               [:file/idx integer?]
               [:file/created-at inst?]
               [:file/name string?]]
   :table     :files
   :namespace :file})

(def projects
  {:fields    #{:team-id :name :id :created-at}
   :spec      [:map
               [:project/team-id uuid?]
               [:project/id uuid?]
               [:project/created-at inst?]
               [:project/name string?]]
   :table     :projects
   :namespace :project})

(def teams
  {:fields    #{:name :type :id :created-at}
   :spec      [:map
               [:team/id uuid?]
               [:team/type keyword?]
               [:team/created-at inst?]
               [:team/name string?]]
   :casts     {:type :team_type}
   :table     :teams
   :namespace :team})

(def user-events
  {:fields    #{:model-id :emitted-by :emitted-at :user-id :id :ctx :event-type :data}
   :spec      [:map
               [:event/emitted-at {:optional true} inst?]
               [:event/emitted-by {:optional true} uuid?]
               [:event/ctx {:optional true} any?]
               [:event/user-id {:optional true} uuid?]
               [:event/id {:optional true} uuid?]
               [:event/model-id {:optional true} uuid?]
               [:event/data {:optional true} any?]
               [:event/event-type {:optional true} string?]]
   :casts     {:ctx :jsonb :data :custom/edn}
   :table     :user-events
   :namespace :event})

(def user-teams
  {:fields    #{:team-id :user-id}
   :spec      [:map
               [:user-team/user-id uuid?]
               [:user-team/team-id uuid?]]
   :table     :user-teams
   :namespace :user-team})

(def users
  {:fields    #{:email :last-name :first-name :mobile-number :id :handle :created-at}
   :spec      [:map
               [:user/id uuid?]
               [:user/created-at inst?]
               [:user/first-name string?]
               [:user/last-name string?]
               [:user/handle string?]
               [:user/email string?]
               [:user/mobile-number string?]]
   :table     :users
   :namespace :user})

(def models
  (maps/->m artifacts
            comments
            event-types
            events
            file-versions
            files
            projects
            teams
            user-events
            user-teams
            users))
