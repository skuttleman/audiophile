(ns com.ben-allred.audiophile.common.views.roots.home
  (:require
    [com.ben-allred.audiophile.common.services.navigation.core :as nav]
    [com.ben-allred.audiophile.common.services.stubs.reagent :as r]
    [com.ben-allred.audiophile.common.utils.logger :as log]
    [com.ben-allred.audiophile.common.views.components.core :as comp]
    [integrant.core :as ig]))

(defn logout [{:keys [nav] :as attrs}]
  [:a (-> attrs
          (select-keys #{:class})
          (assoc :href #?(:cljs "#" :default (nav/path-for nav :auth/logout))
                 :on-click (fn [_]
                             (nav/goto! nav :auth/logout)))
          (cond-> (not (:minimal? attrs)) (update :class conj "button" "is-primary")))
   "Logout"])

(defn project-view [projects state]
  [:div
   [:p [:strong "Your projects"]]
   (if (seq projects)
     [:ul
      (for [{:project/keys [id name]} projects]
        ^{:key id}
        [:li "• " name])]
     [:p "You don't have any projects. Why not create one?"])])

(defn team-view [teams state]
  [:div
   [:p [:strong "Your teams"]]
   (if (seq teams)
     [:ul
      (for [{team-name :team/name :team/keys [id type]} teams]
        ^{:key id}
        [:li "• " team-name (str " - " type)])]
     [:p "You don't have any teams. Why not create one?"])])

(defmethod ig/init-key ::root [_ {:keys [nav projects-tile teams-tile]}]
  (fn [state]
    (if (:auth/user state)
      [:div
       [:p "Welcome, " (get-in state [:auth/user :user/first-name])]
       [:div.level.layout--space-below.layout--xxl.gutters
        {:style {:align-content :flex-start}}
        [projects-tile state project-view]
        [teams-tile state team-view]]]
      (nav/navigate! nav :ui/login))))

(defmethod ig/init-key ::header [_ {:keys [nav]}]
  (fn [_state]
    (let [shown? (r/atom false)]
      (fn [state]
        (let [handler (get-in state [:page :handler])]
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
                   {:class [(when (= :ui/home handler) "is-active")]}
                   [:a.navbar-item {:href (nav/path-for nav :ui/home)} "Home"]]]]]
               [:div.navbar-end.oversize
                [:div.navbar-item
                 [:div.buttons
                  [logout {:nav nav}]]]]])]])))))
