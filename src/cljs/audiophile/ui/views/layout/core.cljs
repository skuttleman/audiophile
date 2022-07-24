(ns audiophile.ui.views.layout.core
  (:require
    [audiophile.ui.components.core :as comp]
    [audiophile.ui.components.modals :as modals]
    [audiophile.ui.components.notices :as not]
    [audiophile.ui.services.login :as login]
    [audiophile.ui.store.queries :as q]
    [audiophile.ui.utils.modulizer :as mod]
    [audiophile.ui.views.layout.services :as serv]
    [reagent.core :as r]))

(def dashboard (mod/lazy-component audiophile.ui.views.dashboard.core/root))

(def file (mod/lazy-component audiophile.ui.views.file.core/root))

(def project (mod/lazy-component audiophile.ui.views.project.core/root))

(def team (mod/lazy-component audiophile.ui.views.team.core/root))

(defn ^:private header [{:keys [nav store] :as sys}]
  (r/with-let [shown? (r/atom false)]
    (let [handle (:handle @nav)
          user (q/user:profile store)
          text (if (= (:token/type user) :token/signup)
                 "Start over"
                 "Logout")
          home (serv/nav#home sys)]
      [:header.header
       [:nav.navbar
        {:role "navigation" :aria-label "main navigation"}
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
            [login/logout {:minimal? true
                           :nav      nav
                           :text     text
                           :class    ["navbar-item"]}]]]
          [:ul.navbar-start.oversize.tabs
           [:li
            {:class [(when (= :routes.ui/home handle) "is-active")]}
            [:a.navbar-item {:href home} "Home"]]]]]
        [:div.navbar-end.oversize
         [:div.navbar-item
          [:div.buttons
           [login/logout {:nav nav :text text}]]]]]])))

(defn ^:private root* [{:keys [nav] :as sys}]
  (let [handle (:handle @nav)
        comp (case handle
               :routes.ui/home @dashboard
               :routes.ui/files:id @file
               :routes.ui/projects:id @project
               :routes.ui/teams:id @team
               comp/not-found)]
    [comp sys]))

(defn root [{:keys [nav store] :as sys}]
  [:div
   [not/banners sys (q/banner:state store)]
   [header sys]
   [:div.main.layout--inset
    {:class [(str "page-" (some-> @nav :handle name))]}
    [:div.layout--inset
     [root* sys]]]
   [modals/root sys (q/modal:state store)]
   [not/toasts sys (q/toast:state store)]])
