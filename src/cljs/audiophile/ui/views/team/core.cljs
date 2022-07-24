(ns audiophile.ui.views.team.core
  (:require
    [audiophile.common.core.utils.logger :as log]
    [audiophile.common.core.utils.maps :as maps]
    [audiophile.common.infrastructure.resources.core :as res]
    [audiophile.ui.components.core :as comp]
    [audiophile.ui.components.input-fields :as in]
    [audiophile.ui.components.modals :as modals]
    [audiophile.ui.forms.core :as forms]
    [audiophile.ui.store.queries :as q]
    [audiophile.ui.views.common.services :as cserv]
    [audiophile.ui.views.dashboard.projects :as cproj]
    [audiophile.ui.views.team.services :as serv]
    [com.ben-allred.vow.core :as v :include-macros true]
    [reagent.core :as r]))

(defn ^:private invite* [sys attrs]
  (r/with-let [*form (serv/teams#form:invite sys attrs (:team-id attrs))]
    [comp/form {:*form *form}
     [in/input (forms/with-attrs {:label       "Email address"
                                  :auto-focus? true}
                                 *form
                                 [:user/email])]]
    (finally
      (forms/destroy! *form))))

(defmethod modals/body ::invite
  [_ sys attrs]
  [invite* sys (cserv/modals#with-on-success attrs)])

(defn ^:private create* [sys attrs]
  (r/with-let [*form (cserv/projects#form:new sys attrs (:team-id attrs))]
    [:div
     [comp/form {:*form *form}
      [in/input (forms/with-attrs {:label       "Name"
                                   :auto-focus? true}
                                  *form
                                  [:project/name])]]]
    (finally
      (forms/destroy! *form))))

(defmethod modals/body ::create-project
  [_ sys attrs]
  (let [attrs (cserv/modals#with-on-success attrs)]
    [create* sys attrs]))

(defn ^:private revoke* [*form]
  [:div
   [:p "Are you sure you want to "
    [:strong [:em "revoke"]]
    " this invitation?"]
   [comp/form {:*form       *form
               :submit/text "Revoke"}]])

(defmethod modals/body ::revoke-invitation
  [_ sys {:keys [email team-id] :as attrs}]
  (r/with-let [attrs (cserv/modals#with-on-success attrs)
               *form (serv/invitations#form:modify sys attrs team-id email)]
    [revoke* *form]
    (finally
      (forms/destroy! *form))))

(defn ^:private invitation-details [sys *team invitation users-invite?]
  (r/with-let [modal-body [::revoke-invitation {:*res *team
                                                :team-id (:team/id invitation)
                                                :email (:team-invitation/email invitation)}]
               click (serv/invitations#modal:revoke sys modal-body)]
    [:li.team-invitation
     [:div.layout--align-center
      (when users-invite?
        [:div.layout--space-after
         [comp/plain-button {:class    ["is-danger" "layout--space-between"]
                             :on-click click}
          [comp/icon :times]]])
      [:span (:team-invitation/email invitation)]]]))

(defn ^:private team-details [{:keys [store] :as sys} *team {:keys [on-invite on-project]} team]
  (let [[title icon] (cserv/teams#type->icon (keyword (:team/type team)))
        personal? (= :PERSONAL (:team/type team))
        user-id (:user/id (q/user:profile store))]
    [:div
     [:h2.subtitle.flex
      [:div.layout--space-after
       [comp/icon {:title title} icon]]
      (:team/name team)]
     [:div {:style {:width "16px"}}]
     [:div.buttons
      (when-not personal?
        [comp/plain-button
         {:class    ["is-primary"]
          :on-click on-invite}
         "Invite new team member"])
      [comp/plain-button
       {:class    ["is-primary"]
        :on-click on-project}
       "Create a project"]]
     (when-not personal?
       [:<>
        [:strong "Team members"]
        [:ul.team-members.layout--space-below
         (for [{member-id :member/id :as member} (:team/members team)]
           ^{:key member-id}
           [:li.team-member (:member/first-name member) " " (:member/last-name member)])]])
     (when (seq (:team/invitations team))
       [:<>
        [:strong "Open invitations"]
        [:ul.team-invitations.layout--stack-between.layout--space-below
         (for [{email :team-invitation/email :as invitation} (:team/invitations team)
               :let [invitation (assoc invitation :team/id (:team/id team))]]
           ^{:key email}
           [invitation-details sys *team invitation (= user-id (:inviter/id invitation))])]])
     [:strong "Team projects"]
     [cproj/project-list {:sys sys} (:team/projects team)]]))

(defn ^:private page [{:keys [nav] :as sys}]
  (r/with-let [team-id (-> @nav :params :team/id)
               *team (serv/teams#res:fetch-one sys team-id)
               *sub (serv/teams#sub:start! sys *team)
               on-invite (serv/teams#modal:invite sys [::invite {:*res    *team
                                                                 :team-id team-id}])
               on-project (cserv/projects#modal:create sys [::create-project {:*res    *team
                                                                              :team-id team-id}])]
    [:div.layout--space-below.layout--xxl.gutters
     [:div {:style {:width "100%"}}
      [comp/with-resource *team [team-details sys *team (maps/->m on-invite on-project)]]]]
    (finally
      (serv/teams#sub:stop! *sub)
      (res/destroy! *team))))

(defn root [sys]
  [page sys])
