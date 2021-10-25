(ns com.ben-allred.audiophile.ui.api.views.signup
  (:require
    [com.ben-allred.audiophile.common.api.navigation.core :as nav]
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

(defn root [{:keys [*int nav]}]
  (fn [_]
    (r/with-let [*form (views/signup-form *int)
                 on-submitted (->on-submitted *int nav)]
      [:div
       [:h1.title "Sign up"]
       [comp/form {:*form        *form
                   :style        {:min-width "300px"}
                   :on-submitted on-submitted}
        [in/input (forms/with-attrs {:label       "Handle"
                                     :auto-focus? true}
                                    *form
                                    [:user/handle])]
        [in/input (forms/with-attrs {:label "First name"}
                                    *form
                                    [:user/first-name])]
        [in/input (forms/with-attrs {:label "Last name"}
                                    *form
                                    [:user/last-name])]
        [in/input (forms/with-attrs {:label "Mobile number"}
                                    *form
                                    [:user/mobile-number])]]])))
