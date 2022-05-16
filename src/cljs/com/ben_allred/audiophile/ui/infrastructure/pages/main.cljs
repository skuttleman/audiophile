(ns com.ben-allred.audiophile.ui.infrastructure.pages.main
  (:require
    [com.ben-allred.audiophile.ui.infrastructure.modulizer :as mod]
    [reagent.core :as r]))

(def dashboard (mod/lazy-component com.ben-allred.audiophile.ui.infrastructure.pages.dashboard/page))

(def other (mod/lazy-component com.ben-allred.audiophile.ui.infrastructure.pages.other/page))

(defn root [_sys _profile]
  (let [state (r/atom {:page :dashboard})]
    (fn [sys profile]
      [:div
       "MAIN"
       [:ul
        [:li [:button.button {:on-click (fn [_]
                                          (swap! state assoc :page :dashboard))}
              "Dashboard"]]
        [:li [:button.button {:on-click (fn [_]
                                          (swap! state assoc :page :other))}
              "Other"]]]
       (case (:page @state)
         :dashboard [@dashboard sys profile]
         [@other sys profile])])))
