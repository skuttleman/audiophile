(ns audiophile.backend.infrastructure.templates.specs
  (:require
    [audiophile.common.domain.validations.specs :as specs]
    [clojure.spec.alpha :as s]
    [clojure.spec.gen.alpha :as gen]
    [clojure.string :as string]))

(defn ^:private trimmed-string-gen [prefix]
  (gen/fmap (partial str prefix) (s/gen string?)))

(def ^:private email-gen
  (gen/fmap #(string/lower-case (str % "@domain.tld")) (s/gen string?)))

(def ^:private phone-gen
  (gen/fmap string/join (gen/vector (gen/elements [\0 \1 \2 \3 \4 \5 \6 \7 \8 \9]) 10)))

(def ^:private specs
  {})

(defn ^:private defspec
  ([kw param-spec]
   (defspec kw param-spec nil))
  ([kw param-spec ret-spec]
   (alter-var-root #'specs assoc [kw ::params] param-spec [kw ::ret] ret-spec)))

(defn ^:private event [event-type & field-specs]
  [event-type
   [:map
    [:event/type [:enum event-type]]
    [:event/data
     (into [:map] field-specs)]]])

(defn param-spec [kw]
  (get specs [kw ::params]))

(defn ret-spec [kw]
  (get specs [kw ::ret]))

;; WF specs

(defspec :artifacts/create
  [:map
   [:artifact/filename {:gen/gen (trimmed-string-gen "f")} specs/trimmed-string?]
   [:artifact/content-type {:gen/gen (trimmed-string-gen "c")} specs/trimmed-string?]
   [:artifact/size nat-int?]
   [:artifact/uri {:gen/gen (gen/fmap str (s/gen uri?))} specs/trimmed-string?]
   [:artifact/key {:gen/gen (gen/fmap str (s/gen uuid?))} specs/trimmed-string?]]
  [:map
   [:artifact/id uuid?]
   [:artifact/filename {:gen/gen (trimmed-string-gen "f")} specs/trimmed-string?]])

(defspec :comments/create
  [:map
   [:comment/file-version-id uuid?]
   [:comment/body {:gen/gen (trimmed-string-gen "b")} specs/trimmed-string?]
   [:comment/selection {:optional true} [:maybe [:tuple number? number?]]]
   [:comment/comment-id {:optional true} [:maybe uuid?]]
   [:user/id uuid?]]
  [:map
   [:comment/id uuid?]])

(defspec :file-versions/activate
  [:map
   [:file-version/id uuid?]])

(defspec :file-versions/create
  [:map
   [:artifact/id uuid?]
   [:file/id uuid?]
   [:file-version/name {:gen/gen (trimmed-string-gen "n")} specs/trimmed-string?]]
  [:map
   [:file-version/id uuid?]])

(defspec :files/create
  [:map
   [:artifact/id uuid?]
   [:project/id uuid?]
   [:file/name {:gen/gen (trimmed-string-gen "n")} specs/trimmed-string?]
   [:file-version/name {:gen/gen (trimmed-string-gen "n")} specs/trimmed-string?]]
  [:map
   [:file/id uuid?]
   [:file-version/id uuid?]])

(defspec :files/update
  [:map
   [:file/id uuid?]
   [:file-version/id uuid?]
   [:file/name {:gen/gen (trimmed-string-gen "n")} specs/trimmed-string?]
   [:file-version/name {:gen/gen (trimmed-string-gen "n")} specs/trimmed-string?]])

(defspec :projects/create
  [:map
   [:project/team-id uuid?]
   [:project/name {:gen/gen (trimmed-string-gen "n")} specs/trimmed-string?]]
  [:map
   [:project/id uuid?]])

(defspec :projects/update
  [:map
   [:project/id uuid?]
   [:project/name {:gen/gen (trimmed-string-gen "n")} specs/trimmed-string?]])

(defspec :team-invitations/create
  [:map
   [:user/id uuid?]
   [:team/id uuid?]
   [:inviter/id uuid?]
   [:user/email {:gen/gen email-gen} specs/email?]])

(defspec :team-invitations/update
  [:map
   [:user/id uuid?]
   [:team/id uuid?]
   [:team-invitation/invited-by uuid?]
   [:team-invitation/email {:gen/gen email-gen} specs/email?]
   [:team-invitation/status specs/team-invitation-status]])

(defspec :teams/create
  [:map
   [:user/id uuid?]
   [:team/name {:gen/gen (trimmed-string-gen "n")} specs/trimmed-string?]
   [:team/type specs/team-type]]
  [:map
   [:team/id uuid?]])

(defspec :teams/update
  [:map
   [:team/id uuid?]
   [:team/name {:gen/gen (trimmed-string-gen "n")} specs/trimmed-string?]])

(defspec :users/signup
  [:map
   [:user/handle {:gen/gen (trimmed-string-gen "h")} specs/trimmed-string?]
   [:user/email {:gen/gen email-gen} specs/email?]
   [:user/first-name {:gen/gen (trimmed-string-gen "fn")} specs/trimmed-string?]
   [:user/last-name {:gen/gen (trimmed-string-gen "ln")} specs/trimmed-string?]
   [:user/mobile-number {:gen/gen phone-gen} specs/phone?]]
  [:map
   [:user/id uuid?]
   [:login/token {:gen/gen (trimmed-string-gen "t")} specs/trimmed-string?]])

;; TASK specs

(defspec :artifact/create!
  [:map
   [:artifact/filename {:gen/gen (trimmed-string-gen "f")} specs/trimmed-string?]
   [:artifact/content-type {:gen/gen (trimmed-string-gen "c")} specs/trimmed-string?]
   [:artifact/size nat-int?]
   [:artifact/uri {:gen/gen (gen/fmap str (s/gen uri?))} specs/trimmed-string?]
   [:artifact/key {:gen/gen (gen/fmap str (s/gen uuid?))} specs/trimmed-string?]]
  [:map
   [:artifact/id uuid?]])

(defspec :comment/create!
  [:map
   [:comment/file-version-id uuid?]
   [:comment/body specs/trimmed-string?]
   [:comment/selection {:optional true} [:maybe [:tuple number? number?]]]
   [:comment/comment-id {:optional true} [:maybe uuid?]]
   [:comment/created-by uuid?]]
  [:map
   [:comment/id uuid?]])

(defspec :file-version/activate!
  [:map
   [:file-version/id uuid?]])

(defspec :file-version/create!
  [:map
   [:artifact/id uuid?]
   [:file/id uuid?]
   [:file-version/name {:gen/gen (trimmed-string-gen "n")} specs/trimmed-string?]]
  [:map
   [:file-version/id uuid?]])

(defspec :file-version/update!
  [:map
   [:file-version/id uuid?]
   [:file-version/name {:gen/gen (trimmed-string-gen "n")} specs/trimmed-string?]])

(defspec :file/create!
  [:map
   [:project/id uuid?]
   [:file/name {:gen/gen (trimmed-string-gen "n")} specs/trimmed-string?]]
  [:map
   [:file/id uuid?]])

(defspec :file/update!
  [:map
   [:file/id uuid?]
   [:file/name {:gen/gen (trimmed-string-gen "n")} specs/trimmed-string?]])

(defspec :project/create!
  [:map
   [:project/team-id uuid?]
   [:project/name {:gen/gen (trimmed-string-gen "n")} specs/trimmed-string?]]
  [:map
   [:project/id uuid?]])

(defspec :project/update!
  [:map
   [:project/id uuid?]
   [:project/name {:gen/gen (trimmed-string-gen "n")} specs/trimmed-string?]])

(defspec :pubsub/publish!
  [:map
   [:events
    [:vector
     [:map
      [:topic
       [:or
        [:tuple keyword?]
        [:tuple keyword? some?]]]
      [:payload
       [:multi {:dispatch :event/type}
        (event :file/updated
               [:file/id uuid?]
               [:file-version/id uuid?]
               [:file/name {:gen/gen (trimmed-string-gen "n")} specs/trimmed-string?]
               [:file-version/name {:gen/gen (trimmed-string-gen "n")} specs/trimmed-string?])
        (event :project/updated
               [:project/id uuid?]
               [:project/name {:gen/gen (trimmed-string-gen "n")} specs/trimmed-string?])
        (event :team-invitation/created
               [:team-invitation/team-id uuid?]
               [:team-invitation/email {:gen/gen email-gen} specs/email?])
        (event :team-invitation/updated
               [:team-invitation/team-id uuid?]
               [:team-invitation/email {:gen/gen email-gen} specs/email?]
               [:team-invitation/status specs/team-invitation-status])
        (event :team/updated
               [:team/id uuid?]
               [:team/name {:gen/gen (trimmed-string-gen "n")} specs/trimmed-string?])]]]]]])

(defspec :team-invitation/create!
  [:map
   [:team-invitation/team-id uuid?]
   [:team-invitation/invited-by uuid?]
   [:team-invitation/email {:gen/gen email-gen} specs/email?]])

(defspec :team-invitation/update!
  [:map
   [:team-invitation/team-id uuid?]
   [:team-invitation/user-id uuid?]
   [:team-invitation/email {:gen/gen email-gen} specs/email?]
   [:team-invitation/status specs/team-invitation-status]])

(defspec :team/create!
  [:map
   [:user/id uuid?]
   [:team/name {:gen/gen (trimmed-string-gen "n")} specs/trimmed-string?]
   [:team/type specs/team-type]]
  [:map
   [:team/id uuid?]])

(defspec :team/update!
  [:map
   [:team/id uuid?]
   [:team/name {:gen/gen (trimmed-string-gen "n")} specs/trimmed-string?]])

(defspec :user/create!
  [:map
   [:user/handle {:gen/gen (trimmed-string-gen "h")} specs/trimmed-string?]
   [:user/email {:gen/gen email-gen} specs/email?]
   [:user/first-name {:gen/gen (trimmed-string-gen "fn")} specs/trimmed-string?]
   [:user/last-name {:gen/gen (trimmed-string-gen "ln")} specs/trimmed-string?]
   [:user/mobile-number {:gen/gen phone-gen} specs/phone?]]
  [:map
   [:user/id uuid?]])

(defspec :user/generate-token!
  [:map
   [:user/id uuid?]
   [:user/handle {:gen/gen (trimmed-string-gen "h")} specs/trimmed-string?]
   [:user/email {:gen/gen email-gen} specs/email?]
   [:user/first-name {:gen/gen (trimmed-string-gen "fn")} specs/trimmed-string?]
   [:user/last-name {:gen/gen (trimmed-string-gen "ln")} specs/trimmed-string?]
   [:user/mobile-number {:gen/gen phone-gen} specs/phone?]]
  [:map
   [:login/token {:gen/gen (trimmed-string-gen "t")} specs/trimmed-string?]])
