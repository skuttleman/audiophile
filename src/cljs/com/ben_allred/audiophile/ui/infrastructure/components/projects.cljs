(ns com.ben-allred.audiophile.ui.infrastructure.components.projects
  (:refer-clojure :exclude [list])
  (:require
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.ui.infrastructure.components.core :as comp]
    [com.ben-allred.audiophile.ui.infrastructure.services.projects :as sproj]
    [com.ben-allred.audiophile.ui.infrastructure.components.input-fields :as in]
    [com.ben-allred.audiophile.common.infrastructure.navigation.core :as nav]))

(defn list [projects {:keys [nav]}]
  [:div
   [:p [:strong "Your projects"]]
   (if (seq projects)
     [:ul.project-list
      (for [{:project/keys [id name]} projects]
        ^{:key id}
        [:li.project-item.layout--space-between
         [:a.link {:href (nav/path-for nav :ui/project {:params {:project/id id}})}
          [:span name]]])]
     [:p "You don't have any projects. Why not create one?"])])

(defn tile [sys]
  (let [*res (sproj/res:list sys)]
    (fn [sys]
      [comp/tile
       [:h2.subtitle "Projects"]
       [comp/with-resource [*res {:spinner/size :small}] list sys]
       [in/plain-button
        {:class    ["is-primary"]
         :on-click (fn [_]
                     (log/warn "TBD"))}
        "Create one"]])))

