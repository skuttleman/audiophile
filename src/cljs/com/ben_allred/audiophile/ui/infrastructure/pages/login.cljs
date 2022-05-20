(ns com.ben-allred.audiophile.ui.infrastructure.pages.login
  (:require
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.common.core.utils.maps :as maps]
    [com.ben-allred.audiophile.common.infrastructure.navigation.core :as nav]
    [com.ben-allred.audiophile.ui.infrastructure.components.core :as comp]
    [com.ben-allred.audiophile.ui.infrastructure.components.input-fields :as in]
    [com.ben-allred.audiophile.ui.infrastructure.components.notices :as not]))

(defn ^:private root* [{:keys [login-form state]}]
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
    [:div.layout--space-above.layout--space-below "Login to get started"]
    [login-form (:nav/route state)]]])

(defn form [{:keys [nav]}]
  (fn [route]
    [:div.buttons
     [in/plain-button
      {:class    ["is-primary"]
       :on-click (fn [_]
                   (nav/goto! nav :auth/login {:params {:redirect-uri (:path route)}}))}
      "Login"]]))

(defn root [{:keys [login-form store] :as sys}]
  (let [state @store]
    [:div
     [not/banners sys state]
     [:div.main.layout--inset
      {:class ["page-login"]}
      [:div.layout--inset
       [root* (maps/->m login-form state)]]]]))
