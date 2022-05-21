(ns audiophile.common.domain.validations.specs
  (:require
    [clojure.set :as set]
    [clojure.string :as string]
    [audiophile.common.core.utils.logger :as log]
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

(def auth
  [:map
   [:user/id uuid?]
   [:token/aud [:fn (partial set/subset? #{:token/auth})]]])

(def signup
  [:map
   [:token/aud [:fn (partial set/subset? #{:token/signup})]]])

(def file-id
  (mu/merge auth [:map [:file/id uuid?]]))

(def project-id
  (mu/merge auth [:map [:project/id uuid?]]))

(def team-id
  (mu/merge auth [:map [:team/id uuid?]]))

(def search
  (mu/merge signup
            [:map
             [:search/field [:fn #{:user/handle :user/mobile-number}]]
             [:search/value trimmed-string?]]))

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

(def event:fetch-all
  [:map
   [:filter/since {:optional true} uuid?]])

(def file:create
  [:map
   [:artifact/id uuid?]
   [:file/name trimmed-string?]
   [:version/name trimmed-string?]])

(def version:create
  [:map
   [:artifact/id uuid?]
   [:version/name trimmed-string?]])

(def project:create
  [:map
   [:project/name trimmed-string?]
   [:project/team-id uuid?]])

(def team:create
  [:map
   [:team/name trimmed-string?]
   [:team/type [:fn #{:PERSONAL :COLLABORATIVE}]]])

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

(def api-ws:connect
  (mu/merge auth
            [:map
             [:token/aud [:fn (fn [s]
                                (or (contains? s :token/auth)
                                    (contains? s :token/signup)))]]
             [:accept trimmed-string?]
             [:content-type trimmed-string?]
             [:websocket? [:fn #{true}]]]))

(def api-event:fetch-all
  (mu/merge auth event:fetch-all))

(def api-file:create
  (mu/merge project-id file:create))

(def api-version:create
  (-> file-id
      (mu/merge [:map [:request/id {:optional true} uuid?]])
      (mu/merge version:create)))

(def api-project:create
  (-> auth
      (mu/merge [:map [:request/id {:optional true} uuid?]])
      (mu/merge project:create)))

(def api-team:create
  (-> auth
      (mu/merge [:map [:request/id {:optional true} uuid?]])
      (mu/merge team:create)))

(def api-user:create
  (-> signup
      (mu/merge [:map
                 [:request/id {:optional true} uuid?]
                 [:user/email [:re #"^[a-z\-\+_0-9\.]+@[a-z\-\+_0-9]+\.[a-z\-\+_0-9\.]+$"]]])
      (mu/merge user:create)))

(def res-version:download
  (mu/merge auth
            [:map
             [:artifact/id uuid?]]))
