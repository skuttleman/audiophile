(ns audiophile.ui.views.dashboard.teams
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

(defn ^:private body* [*form _]
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
    [body* *form attrs]
    (finally
      (forms/destroy! *form))))

(defmethod modals/body ::update
  [_ sys {:keys [*res close! team] :as attrs}]
  (r/with-let [attrs (assoc attrs :on-success (fn [result]
                                                (when close!
                                                  (close! result))
                                                (some-> *res res/request!)))
               *form (serv/teams#form:modify sys attrs team)]
    [body* *form attrs]
    (finally
      (forms/destroy! *form))))

(defn team-item [{:keys [*res sys]} {:team/keys [name type] :as team}]
  (r/with-let [click (serv/teams#modal:update sys [::update {:*res *res :team team}])]
    (let [[title icon] (team-type->icon (keyword type))]
      [:li.layout--row.team-item
       {:style {:display         :flex
                :align-items :center}}
       [:div {:style {:display         :flex
                      :justify-content :center
                      :width           "32px"}}
        [comp/icon {:title title} icon]]
       name
       [comp/plain-button
        {:class    ["is-text"]
         :on-click click}
        [comp/icon :edit]]])))

(defn team-list [attrs teams]
  [:div
   [:p [:strong "Your teams"]]
   (if (seq teams)
     [:ul.team-list
      (for [team teams]
        ^{:key (:team/id team)}
        [team-item attrs team])]
     [:p "You don't have any teams. Why not create one?"])])

(defn tile [sys *res]
  (r/with-let [click (serv/teams#modal:create sys [::create {:*res *res}])]
    [comp/tile
     [:h2.subtitle "Teams"]
     [comp/with-resource [*res {:spinner/size :small}] [team-list {:*res *res
                                                                   :sys  sys}]]
     [comp/plain-button
      {:class    ["is-primary"]
       :on-click click}
      "Create one"]]))
