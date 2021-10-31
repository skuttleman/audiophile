(ns com.ben-allred.audiophile.ui.api.views.teams
  (:refer-clojure :exclude [list])
  (:require
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.ui.api.views.core :as views]
    [com.ben-allred.audiophile.ui.core.components.core :as comp]
    [com.ben-allred.audiophile.ui.core.components.input-fields :as in]
    [com.ben-allred.audiophile.ui.core.forms.core :as forms]
    [com.ben-allred.audiophile.ui.core.utils.reagent :as r]))

(def ^:private team-type->icon
  {:PERSONAL      ["Personal Team" :user]
   :COLLABORATIVE ["Collaborative Team" :users]})

(defn ^:private create* [*int cb]
  (r/with-let [*form (views/team-form *int)]
    [:div
     [comp/form {:*form        *form
                 :on-submitted (views/on-team-created *int cb)}
      [in/input (forms/with-attrs {:label       "Name"
                                   :auto-focus? true}
                                  *form
                                  [:team/name])]]]))

(defn list [_]
  (fn [teams _state]
    [:div
     [:p [:strong "Your teams"]]
     (if (seq teams)
       [:ul.team-list
        (for [{team-name :team/name :team/keys [id type]} teams
              :let [[title icon] (team-type->icon (keyword type))]]
          ^{:key id} [:li.layout--row.team-item
                      [:div {:style {:display         :flex
                                     :justify-content :center
                                     :width           "32px"}}
                       [comp/icon {:title title} icon]]
                      team-name])]
       [:p "You don't have any teams. Why not create one?"])]))

(defn create [{:keys [*int]}]
  (fn [cb]
    [create* *int cb]))
