(ns com.ben-allred.audiophile.common.views.roots.login
  (:require
    #?(:clj  [com.ben-allred.audiophile.common.services.forms.noop :as forms.noop]
       :cljs [com.ben-allred.audiophile.ui.services.forms.standard :as forms.std])
    [com.ben-allred.audiophile.common.services.forms.core :as forms]
    [com.ben-allred.audiophile.common.services.navigation.core :as nav]
    [com.ben-allred.audiophile.common.services.resources.validated :as vres]
    [com.ben-allred.audiophile.common.views.components.core :as comp]
    [com.ben-allred.audiophile.common.views.components.input-fields :as in]
    [com.ben-allred.formation.core :as f]
    [integrant.core :as ig]))

(def validator
  (f/validator {:email (f/required "email is required")}))

(defn create-form []
  #?(:clj  (forms.noop/create)
     :cljs (forms.std/create nil validator)))

(defn root* [res _state]
  (let [form (vres/create res (create-form))]
    (fn [_res state]
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
        [comp/form {:form        form
                    :page        (:page state)
                    :submit/text "Login"}
         [in/input (forms/with-attrs {:label       "email"
                                      :auto-focus? true}
                                     form
                                     [:email])]]]])))

(defmethod ig/init-key ::root [_ {:keys [nav login-resource]}]
  (fn [state]
    (let [redirect-uri (get-in state [:page :query-params :redirect-uri])]
      (if (:auth/user state)
        (nav/-navigate! nav (or redirect-uri "/"))
        [root* login-resource state]))))
