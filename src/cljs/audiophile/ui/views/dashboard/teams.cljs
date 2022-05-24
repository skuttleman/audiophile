(ns audiophile.ui.views.dashboard.teams
  (:refer-clojure :exclude [list])
  (:require
    [audiophile.common.core.utils.logger :as log]
    [audiophile.common.infrastructure.resources.core :as res]
    [audiophile.ui.components.core :as comp]
    [audiophile.ui.components.input-fields :as in]
    [audiophile.ui.components.modals :as modals]
    [audiophile.ui.forms.core :as forms]
    [audiophile.ui.views.dashboard.services :as serv]
    [reagent.core :as r]))

(def ^:private team-type->icon
  {:PERSONAL      ["Personal Team" :user]
   :COLLABORATIVE ["Collaborative Team" :users]})

(defn ^:private create* [*form _]
  [:div
   [comp/form {:*form *form}
    [in/input (forms/with-attrs {:label       "Name"
                                 :auto-focus? true}
                                *form
                                [:team/name])]]])

(defmethod modals/body ::create
  [_ sys {:keys [*res close!] :as attrs}]
  (r/with-let [attrs (assoc attrs :on-success (fn [result]
                                                (when close!
                                                  (close! result))
                                                (some-> *res res/request!)))
               *form (serv/teams#form:new sys attrs)]
    [create* *form attrs]))

(defn list [teams]
  [:div
   [:p [:strong "Your teams"]]
   (if (seq teams)
     [:ul.team-list
      (for [{:team/keys [id name type]} teams
            :let [[title icon] (team-type->icon (keyword type))]]
        ^{:key id}
        [:li.layout--row.team-item
         [:div {:style {:display         :flex
                        :justify-content :center
                        :width           "32px"}}
          [comp/icon {:title title} icon]]
         name])]
     [:p "You don't have any teams. Why not create one?"])])

(defn tile [sys *res]
  (r/with-let [click (serv/teams#modal:create sys [::create {:*res *res}])]
    [comp/tile
     [:h2.subtitle "Teams"]
     [comp/with-resource [*res {:spinner/size :small}] list]
     [comp/plain-button
      {:class    ["is-primary"]
       :on-click click}
      "Create one"]]))
