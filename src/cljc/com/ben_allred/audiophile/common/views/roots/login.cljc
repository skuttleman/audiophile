(ns com.ben-allred.audiophile.common.views.roots.login
  (:require
    #?(:cljs    [com.ben-allred.audiophile.ui.services.forms.standard :as forms.std]
       :default [com.ben-allred.audiophile.common.services.forms.noop :as forms.noop])
    [com.ben-allred.audiophile.common.services.forms.core :as forms]
    [com.ben-allred.audiophile.common.services.navigation.core :as nav]
    [com.ben-allred.audiophile.common.services.resources.validated :as vres]
    [com.ben-allred.audiophile.common.views.components.core :as comp]
    [com.ben-allred.audiophile.common.views.components.input-fields :as in]
    [com.ben-allred.formation.core :as f]
    [com.ben-allred.formation.validations :as vf]
    [integrant.core :as ig]))

(def validator
  (f/validator {:email (vf/required "email is required")}))

(defn create-form []
  #?(:cljs    (forms.std/create nil validator)
     :default (forms.noop/create)))

(defn root* [res _state]
  (let [form (vres/create res (create-form))]
    (fn [_res state]
      [comp/form {:form form :page (:page state) :submit/text "Login"}
       [in/input (forms/with-attrs {:label       "email"
                                    :auto-focus? true}
                                   form
                                   [:email])]])))

(defmethod ig/init-key ::root [_ {:keys [nav login-resource]}]
  (fn [state]
    (let [redirect-uri (get-in state [:page :query-params :redirect-uri])]
      (if (:auth/user state)
        (nav/-navigate! nav (or redirect-uri "/"))
        [root* login-resource state]))))
