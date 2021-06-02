(ns com.ben-allred.audiophile.common.infrastructure.views.roots.home
  (:require
    [com.ben-allred.audiophile.common.app.navigation.core :as nav]
    [com.ben-allred.audiophile.common.core.stubs.reagent :as r]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.common.core.ui-components.core :as comp]
    [com.ben-allred.audiophile.common.core.ui-components.input-fields :as in]))

(defn ^:private logout [{:keys [nav] :as attrs}]
  [:a (-> attrs
          (select-keys #{:class})
          (assoc :href #?(:cljs "#" :default (nav/path-for nav :auth/logout))
                 :on-click (fn [_]
                             (nav/goto! nav :auth/logout)))
          (cond-> (not (:minimal? attrs)) (update :class conj "button" "is-primary")))
   "Logout"])

(defn header [{:keys [nav]}]
  (fn [_state]
    (let [shown? (r/atom false)]
      (fn [state]
        (let [handle (get-in state [:nav/route :handle])]
          [:header.header
           [:nav.navbar
            {:role "navigation" :aria-label "main navigation"}
            (when (:auth/user state)
              [:<>
               [:div.navbar-brand
                [:a.navbar-item {:href (nav/path-for nav :ui/home)}
                 [comp/icon :headphones]]]
               [:div.navbar-start
                {:style {:position :relative}}
                [:span.navbar-burger.burger
                 {:on-click #(swap! shown? not) :cursor :pointer}
                 [:span {:aria-hidden "true"}]
                 [:span {:aria-hidden "true"}]
                 [:span {:aria-hidden "true"}]]
                [:div#header-nav.navbar-menu
                 (when @shown?
                   {:on-click #(reset! shown? false)
                    :class    ["expanded"]})
                 [:ul.navbar-start.undersize
                  [:li
                   [:a.navbar-item {:href (nav/path-for nav :ui/home)} "Home"]]
                  [:li
                   [:hr.nav-divider]]
                  [:li
                   [logout {:minimal? true
                            :nav      nav
                            :class    ["navbar-item"]}]]]
                 [:ul.navbar-start.oversize.tabs
                  [:li
                   {:class [(when (= :ui/home handle) "is-active")]}
                   [:a.navbar-item {:href (nav/path-for nav :ui/home)} "Home"]]]]]
               [:div.navbar-end.oversize
                [:div.navbar-item
                 [:div.buttons
                  [logout {:nav nav}]]]]])]])))))

(defn root [{:keys [nav project-form *modals projects-tile team-form teams-tile]}]
  (fn [state]
    (if (:auth/user state)
      [:div
       [:div.level.layout--space-below.layout--xxl.gutters
        {:style {:align-items :flex-start}}
        [projects-tile
         state
         [in/plain-button
          {:class    ["is-primary"]
           :on-click (comp/modal-opener *modals
                                        "Create project"
                                        project-form)}
          "Create one"]]
        [teams-tile
         state
         [in/plain-button
          {:class    ["is-primary"]
           :on-click (comp/modal-opener *modals
                                        "Create team"
                                        team-form)}
          "Create one"]]]]
      (nav/navigate! nav :ui/login))))
