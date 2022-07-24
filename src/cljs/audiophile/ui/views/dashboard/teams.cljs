(ns audiophile.ui.views.dashboard.teams
  (:require
    [audiophile.common.core.utils.logger :as log]
    [audiophile.common.infrastructure.resources.core :as res]
    [audiophile.ui.components.core :as comp]
    [audiophile.ui.components.input-fields :as in]
    [audiophile.ui.components.modals :as modals]
    [audiophile.ui.forms.core :as forms]
    [audiophile.ui.views.common.services :as cserv]
    [audiophile.ui.views.dashboard.services :as serv]
    [reagent.core :as r]))

(defn ^:private body* [*form _]
  [:div
   [comp/form {:*form *form}
    [in/input (forms/with-attrs {:label       "Name"
                                 :auto-focus? true}
                                *form
                                [:team/name])]]])

(defmethod modals/body ::create
  [_ sys attrs]
  (r/with-let [attrs (cserv/modals#with-on-success attrs)
               *form (serv/teams#form:new sys attrs)]
    [body* *form attrs]
    (finally
      (forms/destroy! *form))))

(defmethod modals/body ::update
  [_ sys {:keys [*res team-id] :as attrs}]
  (r/with-let [attrs (cserv/modals#with-on-success attrs)
               *form (serv/teams#form:modify sys attrs (->> @*res
                                                            (filter (comp #{team-id} :team/id))
                                                            first))]
    [body* *form attrs]
    (finally
      (forms/destroy! *form))))

(defn team-item [sys *res {:team/keys [id name type]}]
  (r/with-let [click (serv/teams#modal:update sys [::update {:*res *res :team-id id}])]
    (let [[title icon] (cserv/teams#type->icon (keyword type))]
      [:li.team-item.layout--space-between.layout--align-center
       [:div.layout--align-center.flex
        [:div {:style {:justify-content :center
                       :width           "32px"}}
         [comp/icon {:title title} icon]]
        [:a.link {:href (serv/teams#nav:ui sys id)}
         [:span name]]]
       [comp/plain-button {:class    ["is-text" "layout--space-between"]
                           :on-click click}
        [comp/icon :edit]
        [:span "edit"]]])))

(defn team-list [sys *res teams]
  [:div
   [:p [:strong "Your teams"]]
   (if (seq teams)
     [:ul.team-list
      (for [team teams]
        ^{:key (:team/id team)}
        [team-item sys *res team])]
     [:p "You don't have any teams. Why not create one?"])])

(defn tile [sys *res]
  (r/with-let [click (serv/teams#modal:create sys [::create {:*res *res}])]
    [comp/tile
     [:h2.subtitle "Teams"]
     [comp/with-resource [*res {:spinner/size :small}] [team-list sys *res]]
     [comp/plain-button
      {:class    ["is-primary"]
       :on-click click}
      "Create one"]]))
