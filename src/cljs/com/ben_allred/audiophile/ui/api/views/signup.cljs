(ns com.ben-allred.audiophile.ui.api.views.signup
  (:require
    [clojure.string :as string]
    [com.ben-allred.audiophile.common.api.navigation.core :as nav]
    [com.ben-allred.audiophile.common.core.resources.core :as res]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.common.core.utils.maps :as maps]
    [com.ben-allred.audiophile.ui.api.views.core :as views]
    [com.ben-allred.audiophile.ui.core.components.core :as comp]
    [com.ben-allred.audiophile.ui.core.components.input-fields :as in]
    [com.ben-allred.audiophile.ui.core.forms.core :as forms]
    [com.ben-allred.audiophile.ui.core.utils.reagent :as r]))

(defn ^:private ->on-submitted [*int nav]
  (views/on-user-created *int
                         (fn [{:login/keys [token]}]
                           (when token
                             (nav/goto! nav :auth/login {:params {:login-token token}})))))

(defn ^:private label-details-dispatch [[status in-use?] _]
  (cond
    (= :requesting status) ::requesting
    (= :error status) ::error
    (and (= :success status) (not in-use?)) ::available
    (and (= :success status) in-use?) ::unavailable))

(defmulti ^:private async-label-details label-details-dispatch)

(defmethod async-label-details :default
  [_ _]
  nil)

(defn ^:private label-retry [on-retry & msg]
  [:<>
   (into [:div.text--error
          [comp/icon :exclamation-circle]]
         msg)
   [:div
    " You could try "
    [:a {:href     "#"
         :on-click on-retry}
     "logging in"]
    " again."]])

(defmethod async-label-details ::requesting
  [_ _]
  [in/spinner])

(defmethod async-label-details ::available
  [_ _]
  [:div.text--success [comp/icon :check-circle]])

(defmethod async-label-details ::unavailable
  [_ {:keys [field on-retry]}]
  [label-retry on-retry " " field " in use."])

(defmethod async-label-details ::error
  [_ {:keys [on-retry]}]
  [label-retry on-retry " Something went wrong."])

(defn ^:private async-label [nav details path]
  (r/with-let [on-retry (fn [_]
                          (nav/goto! nav :auth/logout))]
    (let [field (name (first path))
          label (string/replace (string/capitalize field) #"-" " ")]
      [:div.layout--row label
       [:div.layout--row {:style {:margin-left "8px"}}
        [async-label-details details (maps/->m on-retry field)]]])))

(defn ^:private signup-form [*int nav]
  (r/with-let [*form (views/signup-form *int)
               on-submitted (->on-submitted *int nav)
               on-handle-blur (views/on-blur *int [:user/handle])
               on-mobile-blur (views/on-blur *int [:user/mobile-number])
               *handles (views/field-resource *int [:user/handle])
               *mobiles (views/field-resource *int [:user/mobile-number])]
    (let [handle [(res/status *handles) (:in-use? @*handles)]
          mobile [(res/status *mobiles) (:in-use? @*mobiles)]
          disabled (or (contains? #{:requesting :error} (first handle))
                       (contains? #{:requesting :error} (first mobile))
                       (second handle)
                       (second mobile))]
      [comp/form {:*form        *form
                  :style        {:min-width "300px"}
                  :disabled     disabled
                  :on-submitted on-submitted}
       [in/input (forms/with-attrs {:label       [async-label nav handle [:user/handle]]
                                    :auto-focus? true
                                    :on-blur     on-handle-blur}
                                   *form
                                   [:user/handle])]
       [in/input (forms/with-attrs {:label   [async-label nav mobile [:user/mobile-number]]
                                    :on-blur on-mobile-blur}
                                   *form
                                   [:user/mobile-number])]
       [in/input (forms/with-attrs {:label "First name"}
                                   *form
                                   [:user/first-name])]
       [in/input (forms/with-attrs {:label "Last name"}
                                   *form
                                   [:user/last-name])]])))

(defn root [{:keys [*int nav]}]
  (fn [user]
    [:div
     [:h1.title "Sign up"]
     [:p.layout--space-below
      "Welcome, "
      [:em (:user/email user)]
      ". Tell us a bit about yourself before we get started."]
     [signup-form *int nav]]))
