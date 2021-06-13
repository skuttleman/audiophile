(ns com.ben-allred.audiophile.ui.api.views.teams
  (:refer-clojure :exclude [list])
  (:require
    [com.ben-allred.audiophile.common.core.resources.core :as res]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.common.domain.validations.core :as val]
    [com.ben-allred.audiophile.common.domain.validations.specs :as specs]
    [com.ben-allred.audiophile.ui.api.forms.standard :as form]
    [com.ben-allred.audiophile.ui.api.forms.submittable :as form.sub]
    [com.ben-allred.audiophile.ui.core.components.core :as comp]
    [com.ben-allred.audiophile.ui.core.components.input-fields :as in]
    [com.ben-allred.audiophile.ui.core.forms.core :as forms]
    [com.ben-allred.vow.core :as v :include-macros true]))

(def ^:private validator
  (val/validator {:spec specs/team:create}))

(def ^:private team-type->icon
  {:PERSONAL      ["Personal Team" :user]
   :COLLABORATIVE ["Collaborative Team" :users]})

(defn ^:private create* [*teams _cb]
  (let [*form (form.sub/create *teams (form/create {:team/type :COLLABORATIVE} validator))]
    (fn [_*teams cb]
      [:div
       [comp/form {:*form        *form
                   :on-submitted (fn [vow]
                                   (v/peek vow cb nil))}
        [in/input (forms/with-attrs {:label       "Name"
                                     :auto-focus? true}
                                    *form
                                    [:team/name])]]])))

(defn list [_]
  (fn [teams _state]
    [:div
     [:p [:strong "Your teams"]]
     (if (seq teams)
       [:ul
        (for [{team-name :team/name :team/keys [id type]} teams
              :let [[title icon] (team-type->icon type)]]
          ^{:key id}
          [:li {:style {:display :flex}}
           [:div {:style {:display         :flex
                          :justify-content :center
                          :width           "32px"}}
            [comp/icon {:title title} icon]]
           team-name])]
       [:p "You don't have any teams. Why not create one?"])]))

(defn create [{:keys [*teams done]}]
  (fn [cb]
    [create* *teams (done cb)]))
