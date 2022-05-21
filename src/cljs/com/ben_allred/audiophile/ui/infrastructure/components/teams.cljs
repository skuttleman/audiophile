(ns com.ben-allred.audiophile.ui.infrastructure.components.teams
  (:refer-clojure :exclude [list])
  (:require
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.common.infrastructure.store.core :as store]
    [com.ben-allred.audiophile.ui.infrastructure.components.core :as comp]
    [com.ben-allred.audiophile.ui.infrastructure.components.input-fields :as in]
    [com.ben-allred.audiophile.ui.infrastructure.components.modals :as modals]
    [com.ben-allred.audiophile.ui.infrastructure.forms.core :as forms]
    [com.ben-allred.audiophile.ui.infrastructure.services.teams :as steams]
    [com.ben-allred.audiophile.ui.infrastructure.store.actions :as act]
    [com.ben-allred.vow.core :as v]
    [com.ben-allred.audiophile.common.infrastructure.resources.core :as res]
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
  [_ sys attrs]
  (r/with-let [*form (steams/form:new sys (fn [vow]
                                            (-> vow
                                                (v/peek (:close! attrs))
                                                (v/peek (fn [_]
                                                          (some-> attrs :*res res/request!))))))]
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

(defn tile [{:keys [store]} *res]
  [comp/tile
   [:h2.subtitle "Teams"]
   [comp/with-resource [*res {:spinner/size :small}] list]
   [in/plain-button
    {:class    ["is-primary"]
     :on-click (fn [_]
                 (store/dispatch! store (act/modal:add! [:h1.subtitle "Create a team"]
                                                        [::create {:*res *res}])))}
    "Create one"]])
