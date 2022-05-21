(ns audiophile.ui.pages.main
  (:require
    [audiophile.common.infrastructure.navigation.core :as nav]
    [audiophile.ui.components.core :as comp]
    [audiophile.ui.components.modals :as modals]
    [audiophile.ui.components.notices :as not]
    [audiophile.ui.utils.modulizer :as mod]
    [reagent.core :as r]))

(def dashboard (mod/lazy-component audiophile.ui.pages.dashboard/page))

(defn ^:private logout [{:keys [nav text] :as attrs}]
  [:a (-> attrs
          (select-keys #{:class})
          (assoc :href "#"
                 :on-click (fn [_]
                             (nav/goto! nav :auth/logout)))
          (cond-> (not (:minimal? attrs)) (update :class conj "button" "is-primary")))
   (or text "Logout")])

(defn ^:private header [{:keys [nav]} state]
  (r/with-let [shown? (r/atom false)]
    (let [handle (get-in state [:nav/route :handle])
          user (:user/profile state)
          text (if (= (:token/type user) :token/signup)
                 "Start over"
                 "Logout")
          home (nav/path-for nav :ui/home)]
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
           [logout {:nav nav :text text}]]]]]])))

(defn ^:private root* [sys state]
  (let [handle (get-in state [:nav/route :handle])
        comp (case handle
               :ui/home @dashboard
               comp/not-found)]
    [comp sys state]))

(defn root [{:keys [store] :as sys}]
  (let [state @store]
    [:div
     [not/banners sys (:banners state)]
     [header sys state]
     [:div.main.layout--inset
      {:class [(str "page-" (some-> state (get-in [:nav/route :handle]) name))]}
      [:div.layout--inset
       [root* sys state]]]
     [modals/root sys (:modals state)]
     #_[toasts (:toasts state)]]))
