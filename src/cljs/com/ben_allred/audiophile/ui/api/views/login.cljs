(ns com.ben-allred.audiophile.ui.api.views.login
  (:require
    [com.ben-allred.audiophile.common.api.navigation.core :as nav]
    [com.ben-allred.audiophile.ui.core.components.core :as comp]
    [com.ben-allred.audiophile.ui.core.components.input-fields :as in]))

(defn ^:private root* [login-form state]
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
  (constantly
    [:div.buttons
     [in/plain-button
      {:class ["is-primary"]
       :on-click (fn [_]
                   (nav/goto! nav :auth/login))}
      "Login"]]))

(defn root [{:keys [nav login-form]}]
  (fn [state]
    (let [redirect-uri (get-in state [:nav/route :params :redirect-uri])]
      (if (:auth/user state)
        (nav/navigate! nav (or redirect-uri :ui/home))
        [root* login-form state]))))
