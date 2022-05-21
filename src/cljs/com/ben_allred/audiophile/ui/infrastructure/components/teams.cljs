(ns com.ben-allred.audiophile.ui.infrastructure.components.teams
  (:refer-clojure :exclude [list])
  (:require
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.ui.infrastructure.components.core :as comp]
    [com.ben-allred.audiophile.ui.infrastructure.components.input-fields :as in]
    [com.ben-allred.audiophile.ui.infrastructure.services.teams :as steams]))

(def ^:private team-type->icon
  {:PERSONAL      ["Personal Team" :user]
   :COLLABORATIVE ["Collaborative Team" :users]})

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

(defn tile [sys]
  (let [*res (steams/res:list sys)]
    (fn [_sys]
      [comp/tile
       [:h2.subtitle "Teams"]
       [comp/with-resource [*res {:spinner/size :small}] list]
       [in/plain-button
        {:class    ["is-primary"]
         :on-click (fn [_]
                     (log/warn "TBD"))}
        "Create one"]])))
