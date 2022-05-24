(ns audiophile.ui.views.login.core
  (:require
    [audiophile.common.core.utils.logger :as log]
    [audiophile.common.infrastructure.navigation.core :as nav]
    [audiophile.ui.components.core :as comp]
    [audiophile.ui.components.input-fields :as in]
    [audiophile.ui.components.notices :as not]
    [audiophile.ui.forms.core :as forms]
    [audiophile.ui.views.login.multi :as mlogin]
    [audiophile.ui.views.login.services :as serv]
    [reagent.core :as r]))

(defmethod mlogin/form :default
  [_ {:keys [nav]} route]
  [:div.buttons
   [comp/plain-button
    {:class    ["is-primary"]
     :on-click (fn [_]
                 (nav/goto! nav :auth/login {:params {:redirect-uri (:path route)}}))}
    "Login"]])

(defn ^:private layout [sys {:keys [login-key msg state]}]
  [:div..gutters.layout--xl.layout--xxl.layout--inset
   [:div
    [:h1.title [comp/icon :headphones] " Audiophile"]
    [:h2.subtitle "Collaborate on audio files"]
    [:div.columns.layout--space-between.layout--space-below
     [:div.column.has-background-info
      [:div.has-text-centered {:style {:color :white}} "Leave feedback about a file version"]]
     [:div.column.has-background-info
      [:div.has-text-centered {:style {:color :white}} "Get notified of conversations while you're away"]]
     [:div.column.has-background-info
      [:div.has-text-centered {:style {:color :white}} "Assign follow up tasks to get changes made"]]]]
   [:div.gutters.layout--xxl
    [:div.layout--space-above.layout--space-below msg]
    [mlogin/form login-key sys (:nav/route state)]]])

(defn root [{:keys [store] :as sys} attrs]
  (let [state @store]
    [:div
     [not/banners sys (:banners state)]
     [:div.main.layout--inset
      {:class ["page-login"]}
      [:div.layout--inset
       [layout sys (assoc attrs :state state)]]]]))

(defmethod mlogin/form :signup
  [_ sys route]
  (r/with-let [*form (serv/users#form:signup sys route)]
    [comp/form {:class       ["signup-form"]
                :submit/text "Signup"
                :*form       *form}
     [in/input (forms/with-attrs {:label    "email"
                                  :disabled true}
                                 *form
                                 [:user/email])]
     [in/input (forms/with-attrs {:label       "handle"
                                  :auto-focus? true}
                                 *form
                                 [:user/handle])]
     [in/input (forms/with-attrs {:label "first name"}
                                 *form
                                 [:user/first-name])]
     [in/input (forms/with-attrs {:label "last name"}
                                 *form
                                 [:user/last-name])]
     [in/input (forms/with-attrs {:label "mobile number"}
                                 *form
                                 [:user/mobile-number])]]))
