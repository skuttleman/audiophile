(ns com.ben-allred.audiophile.common.views.roots.login
  (:require
    [com.ben-allred.audiophile.common.services.navigation.core :as nav]
    [com.ben-allred.audiophile.common.views.components.core :as comp]
    [integrant.core :as ig]))

(defn root* [login-form state]
  [:div..gutters.layout--xl.layout--xxl.layout--inset
   [:div
    [:h1.title [comp/icon :headphones] " Audiophile"]
    [:h2.subtitle "Collaborate on audio files"]
    [:div.columns.layout--space-between.layout--space-below
     [:div.column.has-background-info
      [:div.has-text-centered "Leave feedback about a file version"]]
     [:div.column.has-background-info
      [:div.has-text-centered "Get notified of conversations while you're away"]]
     [:div.column.has-background-info
      [:div.has-text-centered "Assign follow up tasks to get changes made"]]]]
   [:div.gutters.layout--xxl
    [:div "Login to get started"]
    [login-form (:page state)]]])

(defmethod ig/init-key ::login-form [_ {:keys [nav]}]
  (constantly
    [:div.buttons
     [:button.button.is-primary
      {:on-click (fn [_]
                   (nav/goto! nav :auth/login))}
      "Login"]]))

(defmethod ig/init-key ::root [_ {:keys [nav login-form]}]
  (fn [state]
    (let [redirect-uri (get-in state [:page :query-params :redirect-uri])]
      (if (:auth/user state)
        (nav/-navigate! nav (or redirect-uri "/"))
        [root* login-form state]))))
