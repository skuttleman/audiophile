(ns audiophile.ui.views.team.core
  (:require
    [audiophile.common.core.utils.logger :as log]
    [audiophile.common.core.utils.maps :as maps]
    [audiophile.common.infrastructure.resources.core :as res]
    [audiophile.ui.components.core :as comp]
    [audiophile.ui.components.input-fields :as in]
    [audiophile.ui.components.modals :as modals]
    [audiophile.ui.forms.core :as forms]
    [audiophile.ui.views.common.services :as cserv]
    [audiophile.ui.views.dashboard.projects :as cproj]
    [audiophile.ui.views.team.services :as serv]
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
  [_ sys {:keys [close!] :as attrs}]
  (let [attrs (assoc attrs :on-success (fn [result]
                                         (when close!
                                           (close! result))))]
    [invite* sys attrs]))

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
  [_ sys {:keys [*res close!] :as attrs}]
  (let [attrs (assoc attrs :on-success (fn [result]
                                         (when close!
                                           (close! result))
                                         (some-> *res res/request!)))]
    [create* sys attrs]))

(defn ^:private team-details [sys {:keys [on-invite on-project]} team]
  (let [[title icon] (cserv/teams#type->icon (keyword (:team/type team)))]
    [:div
     [:h2.subtitle.flex
      [:div.layout--space-after
       [comp/icon {:title title} icon]]
      (:team/name team)]
     [:div {:style {:width "16px"}}]
     [:div.buttons
      (when-not (= :PERSONAL (:team/type team))
        [comp/plain-button
         {:class    ["is-primary"]
          :on-click on-invite}
         "Invite new team member"])
      [comp/plain-button
       {:class    ["is-primary"]
        :on-click on-project}
       "Create a project"]]
     (when-not (= :PERSONAL (:team/type team))
       [:<>
        [:strong "Team members"]
        [:ul.team-members
         (for [{member-id :member/id :as member} (:team/members team)]
           ^{:key member-id}
           [:li.team-member (:member/first-name member) " " (:member/last-name member)])]])
     [:strong "Team projects"]
     [cproj/project-list {:sys sys} (:team/projects team)]]))

(defn ^:private page [{:keys [nav] :as sys}]
  (r/with-let [team-id (-> @nav :params :team/id)
               *team (serv/teams#res:fetch-one sys team-id)
               *sub (serv/teams#sub:start! sys *team)
               on-invite (serv/teams#modal:invite sys [::invite {:team-id team-id}])
               on-project (cserv/projects#modal:create sys [::create-project {:*res    *team
                                                                              :team-id team-id}])]
    [:div.layout--space-below.layout--xxl.gutters
     [:div {:style {:width "100%"}}
      [comp/with-resource *team [team-details sys (maps/->m on-invite on-project)]]]]
    (finally
      (serv/teams#sub:stop! *sub)
      (res/destroy! *team))))

(defn root [sys]
  [page sys])
