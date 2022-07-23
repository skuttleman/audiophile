(ns audiophile.ui.views.team.services
  (:require
    [audiophile.common.api.pubsub.core :as pubsub]
    [audiophile.common.core.utils.maps :as maps]
    [audiophile.common.domain.validations.core :as val]
    [audiophile.common.domain.validations.specs :as specs]
    [audiophile.common.infrastructure.resources.core :as res]
    [audiophile.ui.forms.standard :as form.std]
    [audiophile.ui.services.pages :as pages]))

(def ^:private teams#validator:invite
  (val/validator {:spec specs/team-invitation:create}))

(defn teams#form:invite [{:keys [store] :as sys} attrs team-id]
  (let [*form (form.std/create store {:team/id team-id} teams#validator:invite)]
    (pages/form:new sys attrs *form :routes.api/team-invitations)))

(defn invitations#form:modify [{:keys [store] :as sys} attrs team-id email]
  (let [*form (form.std/create store {:team-invitation/team-id team-id
                                      :team-invitation/email   email
                                      :team-invitation/status  :REVOKED})]
    (pages/form:modify sys attrs *form :routes.api/team-invitations)))

(defn invitations#modal:revoke [sys body]
  (pages/modal:open sys [:h1.subtitle "Revoke team invitation"] body))

(defn teams#modal:invite [sys body]
  (pages/modal:open sys [:h1.subtitle "Invite a new team member"] body))

(defn teams#res:fetch-one [sys team-id]
  (pages/res:fetch sys :routes.api/teams:id {:params {:team/id team-id}}))

(defn teams#sub:start! [{:keys [nav pubsub]} *team]
  (let [team-id (-> @nav :params :team/id)]
    (pubsub/subscribe! pubsub
                       ::pages/sub
                       [:teams team-id]
                       (fn [_]
                         (res/request! *team)))
    (maps/->m team-id pubsub)))

(defn teams#sub:stop! [{:keys [team-id pubsub]}]
  (pubsub/unsubscribe! pubsub ::pages/sub [:teams team-id]))
