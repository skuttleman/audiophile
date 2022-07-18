(ns audiophile.ui.views.dashboard.invitations
  (:require
    [audiophile.common.core.utils.logger :as log]
    [audiophile.common.core.utils.strings :as strings]
    [audiophile.common.infrastructure.resources.core :as res]
    [audiophile.ui.components.core :as comp]
    [audiophile.ui.components.modals :as modals]
    [audiophile.ui.forms.core :as forms]
    [audiophile.ui.views.dashboard.services :as serv]
    [reagent.core :as r]
    [audiophile.common.core.utils.maps :as maps]))

(defn ^:private invitation* [{:keys [mode-text]} *form]
  [:div
   [:p "Are you sure you want to "
    [:strong [:em mode-text]]
    " this invitation?"]
   [comp/form {:*form       *form
               :submit/text (strings/capitalize mode-text)}]])

(defmethod modals/body ::handle-invitation
  [_ sys {:keys [*res close!] :as attrs}]
  (r/with-let [attrs (assoc attrs :on-success (fn [result]
                                                (when close!
                                                  (close! result))
                                                (some-> *res res/request!)))
               *form (serv/invitations#form:modify sys attrs)]
    [invitation* attrs *form]
    (finally
      (forms/destroy! *form))))

(defn ^:private invitation-item [clicker invitation]
  [:li.team-item
   [:div.layout--space-between.layout--align-center
    [:div.layout--align-center.flex
     [:div.layout--col
      [:span (:team/name invitation)]
      [:small.layout--inset
       "invited by "
       [:strong
        (:inviter/first-name invitation)
        " "
        (:inviter/last-name invitation)]]]]
    [:div.buttons
     [comp/plain-button {:class    ["is-primary" "layout--space-between"]
                         :on-click (clicker :accept (:team/id invitation))}
      [comp/icon :check]]
     [comp/plain-button {:class    ["is-danger" "layout--space-between"]
                         :on-click (clicker :reject (:team/id invitation))}
      [comp/icon :times]]]]])

(defn invitation-list [clicker invitations]
  [:div
   [:p [:strong "Other teams"]]
   (if (seq invitations)
     [:ul.team-list
      (for [invitation invitations]
        ^{:key (:team/id invitation)}
        [invitation-item clicker invitation])]
     [:p "No pending invitations"])])

(defn tile [sys *projects *teams]
  (r/with-let [*invitations (serv/invitations#res:fetch-all sys)
               modal-body [::handle-invitation (maps/->m *invitations *projects *teams)]
               clicker (serv/invitations#modal:confirm sys modal-body)]
    [comp/tile
     [:h2.subtitle "Pending Invitations"]
     [comp/with-resource [*invitations {:spinner/size :small}] [invitation-list clicker]]]
    (finally
      (res/destroy! *invitations))))
