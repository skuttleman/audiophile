(ns com.ben-allred.audiophile.common.views.roots.home
  (:require
    [com.ben-allred.audiophile.common.services.navigation.core :as nav]
    [com.ben-allred.audiophile.common.services.stubs.reagent :as r]
    [com.ben-allred.audiophile.common.services.ui-store.actions :as actions]
    [com.ben-allred.audiophile.common.services.ui-store.core :as ui-store]
    [com.ben-allred.audiophile.common.utils.logger :as log]
    [com.ben-allred.audiophile.common.views.components.core :as comp]
    [com.ben-allred.audiophile.common.views.components.input-fields :as in]
    [integrant.core :as ig]))

(defn logout [{:keys [nav] :as attrs}]
  [:a (-> attrs
          (select-keys #{:class})
          (assoc :href #?(:cljs "#" :default (nav/path-for nav :auth/logout))
                 :on-click (fn [_]
                             (nav/goto! nav :auth/logout)))
          (cond-> (not (:minimal? attrs)) (update :class conj "button" "is-primary")))
   "Logout"])

(defn ^:private clicker [store view title]
  (fn [_]
    (ui-store/dispatch! store
                        (actions/modal! [:h2.subtitle title]
                                        [view]))))

(defmethod ig/init-key ::root [_ {:keys [nav project-form projects-tile store team-form teams-tile]}]
  (fn [state]
    (if (:auth/user state)
      [:div
       [:p "Welcome, " (get-in state [:auth/user :user/first-name])]
       [:div.level.layout--space-below.layout--xxl.gutters
        {:style {:align-content :flex-start}}
        [projects-tile
         state
         [in/plain-button
          {:class ["is-primary"]
           :on-click (clicker store project-form "Create project")}
          "Create one"]]
        [teams-tile
         state
         [in/plain-button
          {:class ["is-primary"]
           :on-click (clicker store team-form "Create team")}
          "Create one"]]]]
      (nav/navigate! nav :ui/login))))

(defmethod ig/init-key ::header [_ {:keys [nav]}]
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
