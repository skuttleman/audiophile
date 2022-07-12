(ns audiophile.ui.views.dashboard.services
  (:require
    [audiophile.common.core.utils.logger :as log]
    [audiophile.common.domain.validations.core :as val]
    [audiophile.common.domain.validations.specs :as specs]
    [audiophile.common.infrastructure.navigation.core :as nav]
    [audiophile.ui.forms.standard :as form.std]
    [audiophile.ui.services.pages :as pages]))

(def ^:private projects#validator:new
  (val/validator {:spec specs/project:create}))

(def ^:private projects#validator:modify
  (val/validator {:spec specs/project:update}))

(def ^:private teams#validator:new
  (val/validator {:spec specs/team:create}))

(def ^:private teams#validator:modify
  (val/validator {:spec specs/team:update}))

(defn projects#res:fetch-all [sys]
  (pages/res:fetch sys :routes.api/projects))

(defn projects#form:new [{:keys [store] :as sys} attrs team-id]
  (let [*form (form.std/create store {:project/team-id team-id} projects#validator:new)]
    (pages/form:new sys attrs *form :routes.api/projects)))

(defn projects#form:modify [{:keys [store] :as sys} attrs {project-id :project/id :as project}]
  (let [*form (form.std/create store
                               (val/select-keys specs/project:update project)
                               projects#validator:modify)]
    (pages/form:modify sys attrs *form :routes.api/projects:id {:params {:project/id project-id}})))

(defn projects#modal:create [sys body]
  (pages/modal:open sys [:h1.subtitle "Create a project"] body))

(defn projects#modal:update [sys body]
  (pages/modal:open sys [:h1.subtitle "Edit project"] body))

(defn projects#nav:ui [{:keys [nav]} project-id]
  (nav/path-for nav :routes.ui/projects:id {:params {:project/id project-id}}))

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
