(ns com.ben-allred.audiophile.ui.app.views.home
  (:require
    [com.ben-allred.audiophile.common.app.navigation.core :as nav]
    [com.ben-allred.audiophile.ui.core.utils.reagent :as r]
    [com.ben-allred.audiophile.ui.core.components.core :as comp]
    [com.ben-allred.audiophile.ui.core.components.input-fields :as in]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]))

(defn ^:private logout [{:keys [nav] :as attrs}]
  [:a (-> attrs
          (select-keys #{:class})
          (assoc :href "#"
                 :on-click (fn [_]
                             (nav/goto! nav :auth/logout)))
          (cond-> (not (:minimal? attrs)) (update :class conj "button" "is-primary")))
   "Logout"])

(defn header [{:keys [nav]}]
  (fn [_state]
    (let [shown? (r/atom false)]
      (fn [state]
        (let [handle (get-in state [:nav/route :handle])
              home (nav/path-for nav :ui/home)]
          [:header.header
           [:nav.navbar
            {:role "navigation" :aria-label "main navigation"}
            (when (:auth/user state)
              [:<>
               [:div.navbar-brand
                [:a.navbar-item {:href home}
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
                   [:a.navbar-item {:href home} "Home"]]
                  [:li
                   [:hr.nav-divider]]
                  [:li
                   [logout {:minimal? true
                            :nav      nav
                            :class    ["navbar-item"]}]]]
                 [:ul.navbar-start.oversize.tabs
                  [:li
                   {:class [(when (= :ui/home handle) "is-active")]}
                   [:a.navbar-item {:href home} "Home"]]]]]
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
