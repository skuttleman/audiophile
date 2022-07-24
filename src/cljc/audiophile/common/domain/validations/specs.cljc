(ns audiophile.common.domain.validations.specs
  (:require
    [audiophile.common.core.utils.logger :as log]
    [clojure.set :as set]
    [clojure.string :as string]
    [malli.util :as mu])
  #?(:clj
     (:import
       (java.io File))))

(def trimmed-string?
  [:fn (fn [s]
         (and (string? s)
              (= s (not-empty (string/trim s)))))])

(def file?
  [:fn (fn [f]
         #?(:cljs    false
            :default (instance? File f)))])

(def email?
  [:re #"^[a-z\-\+_0-9\.]+@[a-z\-\+_0-9]+\.[a-z\-\+_0-9\.]+$"])

(def auth
  [:map
   [:user/id uuid?]
   [:token/aud [:fn (partial set/subset? #{:token/auth})]]])

(def signup
  [:map
   [:token/aud [:fn (partial set/subset? #{:token/signup})]]])

(def profile
  [:map
   [:user/id uuid?]
   [:user/email {:optional true} email?]
   [:token/aud [:fn (partial some #{:token/auth :token/signup})]]])

(def file-id
  (mu/merge auth [:map [:file/id uuid?]]))

(def project-id
  (mu/merge auth [:map [:project/id uuid?]]))

(def team-id
  (mu/merge auth [:map [:team/id uuid?]]))

(def artifact:create
  [:map
   [:artifact/filename trimmed-string?]
   [:artifact/content-type trimmed-string?]
   [:artifact/tempfile file?]
   [:artifact/size nat-int?]])

(def comment:create
  [:map
   [:comment/file-version-id uuid?]
   [:comment/body trimmed-string?]
   [:comment/selection {:optional true} [:tuple number? number?]]
   [:comment/comment-id {:optional true} uuid?]])

(def comment:fetch-all
  [:map
   [:file-version/id {:optional true} uuid?]])

(def event:fetch-all
  [:map
   [:filter/since {:optional true} uuid?]])

(def file:create
  [:map
   [:artifact/id uuid?]
   [:file/name trimmed-string?]
   [:version/name trimmed-string?]])

(def file:select-version
  [:map
   [:file-version/id uuid?]])

(def version:create
  [:map
   [:artifact/id uuid?]
   [:version/name trimmed-string?]])

(def project:create
  [:map
   [:project/name trimmed-string?]
   [:project/team-id uuid?]])

(def project:update
  [:map
   [:project/name trimmed-string?]])

(def team-invitation:create
  [:map
   [:user/email email?]])

(def team-invitation:update
  [:map
   [:team-invitation/team-id uuid?]
   [:team-invitation/email {:optional true} email?]
   [:team-invitation/status [:fn #{:ACCEPTED :REJECTED :REVOKED}]]])

(def team:create
  [:map
   [:team/name trimmed-string?]
   [:team/type [:fn #{:PERSONAL :COLLABORATIVE}]]])

(def team:update
  [:map
   [:team/name trimmed-string?]])

(def user:create
  [:map
   [:user/first-name trimmed-string?]
   [:user/last-name trimmed-string?]
   [:user/handle trimmed-string?]
   [:user/mobile-number [:re #"^\d{10}$"]]])

(def api-artifact:create
  (-> auth
      (mu/merge [:map
                 [:request/id {:optional true} uuid?]])
      (mu/merge artifact:create)))

(def api-comment:create
  (-> auth
      (mu/merge [:map [:request/id {:optional true} uuid?]])
      (mu/merge comment:create)))

(def api-comment:fetch-all
  (-> file-id
      (mu/merge [:map [:request/id {:optional true} uuid?]])
      (mu/merge comment:fetch-all)))

(def api-ws:connect
  (mu/merge auth
            [:map
             [:token/aud [:fn (partial some #{:token/auth :token/signup})]]
             [:accept trimmed-string?]
             [:content-type trimmed-string?]
             [:websocket? [:fn #{true}]]]))

(def api-event:fetch-all
  (mu/merge auth event:fetch-all))

(def api-file:create
  (-> project-id
      (mu/merge [:map [:request/id {:optional true} uuid?]])
      (mu/merge file:create)))

(def api-file:select-version
  (-> file-id
      (mu/merge [:map [:request/id {:optional true} uuid?]])
      (mu/merge file:select-version)))

(def api-version:create
  (-> file-id
      (mu/merge [:map [:request/id {:optional true} uuid?]])
      (mu/merge version:create)))

(def api-project:create
  (-> auth
      (mu/merge [:map [:request/id {:optional true} uuid?]])
      (mu/merge project:create)))

(def api-project:update
  (-> auth
      (mu/merge [:map
                 [:project/id uuid?]
                 [:request/id {:optional true} uuid?]])
      (mu/merge project:update)))

(def api-team-invitation:create
  (-> auth
      (mu/merge [:map
                 [:team/id uuid?]
                 [:request/id {:optional true} uuid?]])
      (mu/merge team-invitation:create)))

(def api-team-invitation:update
  (-> auth
      (mu/merge [:map
                 [:request/id {:optional true} uuid?]])
      (mu/merge team-invitation:update)))

(def api-team:create
  (-> auth
      (mu/merge [:map [:request/id {:optional true} uuid?]])
      (mu/merge team:create)))

(def api-team:update
  (-> auth
      (mu/merge [:map
                 [:team/id uuid?]
                 [:request/id {:optional true} uuid?]])
      (mu/merge team:update)))

(def api-user:create
  (-> signup
      (mu/merge [:map
                 [:request/id {:optional true} uuid?]
                 [:user/email email?]])
      (mu/merge user:create)))

(def res-version:download
  (mu/merge auth
            [:map
             [:artifact/id uuid?]]))
