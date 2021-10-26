(ns com.ben-allred.audiophile.ui.api.views.signup
  (:require
    [clojure.string :as string]
    [com.ben-allred.audiophile.common.api.navigation.core :as nav]
    [com.ben-allred.audiophile.common.core.resources.core :as res]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
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

(defn ^:private async-label [status path]
  (let [field (name (first path))
        label (string/replace (string/capitalize field) #"-" " ")]
    [:div.layout--row label
     (when-let [body (case status
                       :requesting
                       [in/spinner]

                       :error
                       [:div.text--error
                        [comp/icon :exclamation-circle]
                        " "
                        field
                        " in use"]

                       :success
                       [:div.text--success [comp/icon :check-circle]]

                       nil)]
       [:div {:style {:margin-left "8px"}} body])]))

(defn ^:private signup-form [*int nav]
  (r/with-let [*form (views/signup-form *int)
               on-submitted (->on-submitted *int nav)
               on-handle-blur (views/on-blur *int [:user/handle])
               on-mobile-blur (views/on-blur *int [:user/mobile-number])
               *handles (views/field-resource *int [:user/handle])
               *mobiles (views/field-resource *int [:user/mobile-number])]
    (let [handle-status (res/status *handles)
          mobile-status (res/status *mobiles)]
      [comp/form {:*form        *form
                  :style        {:min-width "300px"}
                  :disabled     (or (contains? #{:requesting :error} handle-status)
                                    (contains? #{:requesting :error} mobile-status))
                  :on-submitted on-submitted}
       [in/input (forms/with-attrs {:label       [async-label handle-status [:user/handle]]
                                    :auto-focus? true
                                    :on-blur     on-handle-blur}
                                   *form
                                   [:user/handle])]
       [in/input (forms/with-attrs {:label   [async-label mobile-status [:user/mobile-number]]
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
