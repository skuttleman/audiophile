(ns com.ben-allred.audiophile.ui.infrastructure.pages.dashboard
  (:require
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.ui.infrastructure.components.input-fields :as in]))

(defn page [sys state]
  [:div
   [:div.level.layout--space-below.layout--xxl.gutters
    {:style {:align-items :flex-start}}
    "DASHY"
    #_[projects-tile
     state
     [in/plain-button
      {:class    ["is-primary"]
       :on-click (comp/modal-opener *modals
                                    "Create project"
                                    project-form)}
      "Create one"]]
    #_[teams-tile
     state
     [in/plain-button
      {:class    ["is-primary"]
       :on-click (comp/modal-opener *modals
                                    "Create team"
                                    team-form)}
      "Create one"]]]])
