(ns com.ben-allred.audiophile.ui.api.views.home
  (:require
    [com.ben-allred.audiophile.common.api.navigation.core :as nav]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.ui.core.components.core :as comp]
    [com.ben-allred.audiophile.ui.core.components.input-fields :as in]
    [com.ben-allred.audiophile.ui.core.utils.reagent :as r]))

(defn ^:private logout [{:keys [nav text] :as attrs}]
  [:a (-> attrs
          (select-keys #{:class})
          (assoc :href "#"
                 :on-click (fn [_]
                             (nav/goto! nav :auth/logout)))
          (cond-> (not (:minimal? attrs)) (update :class conj "button" "is-primary")))
   (or text "Logout")])

(defn header [{:keys [nav]}]
  (fn [state]
    (r/with-let [shown? (r/atom false)]
      (let [handle (get-in state [:nav/route :handle])
            user (:auth/user state)
            text (if (= (:token/type user) :token/signup)
                   "Start over"
                   "Logout")
            home (nav/path-for nav :ui/home)]
        [:header.header
         [:nav.navbar
          {:role "navigation" :aria-label "main navigation"}
          (when user
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
                          :text     text
                          :class    ["navbar-item"]}]]]
               [:ul.navbar-start.oversize.tabs
                [:li
                 {:class [(when (= :ui/home handle) "is-active")]}
                 [:a.navbar-item {:href home} "Home"]]]]]
             [:div.navbar-end.oversize
              [:div.navbar-item
               [:div.buttons
                [logout {:nav nav :text text}]]]]])]]))))

(defn root [{:keys [nav project-form *modals projects-tile signup-form team-form teams-tile]}]
  (fn [state]
    (let [user (:auth/user state)]
      (cond
        (= (:token/type user) :token/signup)
        [signup-form user]

        user
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

        :else
        (nav/navigate! nav :ui/login)))))
