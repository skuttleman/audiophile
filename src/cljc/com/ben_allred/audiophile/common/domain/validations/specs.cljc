(ns com.ben-allred.audiophile.common.domain.validations.specs
  (:require
    [clojure.string :as string]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
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
   [:user/id uuid?]])

(def file-id
  (mu/merge auth [:map [:file/id uuid?]]))

(def project-id
  (mu/merge auth [:map [:project/id uuid?]]))

(def team-id
  (mu/merge auth [:map [:team/id uuid?]]))

(def artifact:create
  [:map
   [:filename trimmed-string?]
   [:content-type trimmed-string?]
   [:tempfile file?]
   [:size nat-int?]])

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

(def api-artifact:create
  (-> auth
      (mu/merge [:map [:request/id {:optional true} uuid?]])
      (mu/merge artifact:create)))

(def api-ws:connect
  (mu/merge auth
            [:map
             [:accept trimmed-string?]
             [:content-type trimmed-string?]
             [:websocket? [:fn #{true}]]]))

(def api-file:create
  (mu/merge project-id file:create))

(def api-version:create
  (mu/merge file-id version:create))

(def api-project:create
  (mu/merge auth project:create))

(def api-team:create
  (mu/merge auth team:create))

(def res-version:download
  (mu/merge auth
            [:map
             [:artifact/id uuid?]]))
