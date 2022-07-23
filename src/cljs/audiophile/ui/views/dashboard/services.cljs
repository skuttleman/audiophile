(ns audiophile.ui.views.dashboard.services
  (:require
    [audiophile.common.core.utils.logger :as log]
    [audiophile.common.domain.validations.core :as val]
    [audiophile.common.domain.validations.specs :as specs]
    [audiophile.common.infrastructure.navigation.core :as nav]
    [audiophile.ui.forms.standard :as form.std]
    [audiophile.ui.resources.impl :as ires]
    [audiophile.ui.services.pages :as pages]))

(def ^:private projects#validator:modify
  (val/validator {:spec specs/project:update}))

(def ^:private teams#validator:new
  (val/validator {:spec specs/team:create}))

(def ^:private teams#validator:modify
  (val/validator {:spec specs/team:update}))

(defn invitations#res:fetch-all [sys]
  (pages/res:fetch sys :routes.api/team-invitations))

(defn invitations#form:modify [{:keys [store] :as sys} {:keys [status team-id] :as attrs}]
  (let [*form (form.std/create store {:team-invitation/team-id team-id
                                      :team-invitation/status  status})]
    (pages/form:modify sys attrs *form :routes.api/team-invitations)))

(defn invitations#modal:confirm [sys [k attrs]]
  (fn [mode team-id]
    (let [attrs' (merge attrs
                        {:mode-text (name mode)
                         :team-id   team-id}
                        (case mode
                          :accept {:*res   (ires/multi (select-keys attrs #{:*invitations
                                                                            :*projects
                                                                            :*teams}))
                                   :status :ACCEPTED}
                          :reject {:*res   (:*invitations attrs)
                                   :status :REJECTED}))]
      (pages/modal:open sys [:h1.subtitle "Team invitation"] [k attrs']))))

(defn projects#res:fetch-all [sys]
  (pages/res:fetch sys :routes.api/projects))

(defn projects#form:modify [{:keys [store] :as sys} attrs {project-id :project/id :as project}]
  (let [*form (form.std/create store
                               (val/select-keys specs/project:update project)
                               projects#validator:modify)]
    (pages/form:modify sys attrs *form :routes.api/projects:id {:params {:project/id project-id}})))

(defn projects#modal:update [sys body]
  (pages/modal:open sys [:h1.subtitle "Edit project"] body))

(defn projects#nav:ui [{:keys [nav]} project-id]
  (nav/path-for nav :routes.ui/projects:id {:params {:project/id project-id}}))

(defn teams#nav:ui [{:keys [nav]} team-id]
  (nav/path-for nav :routes.ui/teams:id {:params {:team/id team-id}}))

(defn teams#res:fetch-all [sys]
  (pages/res:fetch sys :routes.api/teams))

(defn teams#form:new [{:keys [store] :as sys} attrs]
  (let [*form (form.std/create store {:team/type :COLLABORATIVE} teams#validator:new)]
    (pages/form:new sys attrs *form :routes.api/teams)))

(defn teams#form:modify [{:keys [store] :as sys} attrs {team-id :team/id :as team}]
  (let [*form (form.std/create store
                               (val/select-keys specs/team:update team)
                               teams#validator:modify)]
    (pages/form:modify sys attrs *form :routes.api/teams:id {:params {:team/id team-id}})))

(defn teams#modal:create [sys body]
  (pages/modal:open sys [:h1.subtitle "Create a team"] body))

(defn teams#modal:update [sys body]
  (pages/modal:open sys [:h1.subtitle "Edit team"] body))
