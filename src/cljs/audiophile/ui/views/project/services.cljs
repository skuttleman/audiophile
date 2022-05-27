(ns audiophile.ui.views.project.services
  (:require
    [audiophile.common.core.utils.logger :as log]
    [audiophile.common.domain.validations.core :as val]
    [audiophile.common.domain.validations.specs :as specs]
    [audiophile.common.infrastructure.navigation.core :as nav]
    [audiophile.ui.forms.standard :as form.std]
    [audiophile.ui.views.common.services :as cserv]
    [audiophile.ui.services.pages :as pages]))

(def ^:private ui-file-spec
  (conj cserv/ui-version-spec [:file/name specs/trimmed-string?]))

(def ^:private files#validator:new
  (val/validator {:spec ui-file-spec}))

(defn files#form:new [{:keys [store] :as sys} attrs project-id]
  (let [*form (form.std/create store nil files#validator:new)
        attrs (assoc attrs :local->remote cserv/files#local->remove)]
    (pages/form:new sys attrs *form :routes.api/projects:id.files {:params {:project/id project-id}})))

(defn files#modal:create [sys body]
  (pages/modal:open sys [:h1.subtitle "Add a track"] body))

(defn files#modal:version [sys body]
  (pages/modal:open sys [:h1.subtitle "Upload a new version"] body))

(defn files#nav:one [{:keys [nav]} file-id]
  (nav/path-for nav :routes.ui/files:id {:params {:file/id file-id}}))

(defn files#res:fetch-all [sys project-id]
  (pages/res:fetch sys :routes.api/projects:id.files {:params {:project/id project-id}}))

(defn projects#res:fetch-one [sys project-id]
  (pages/res:fetch sys :routes.api/projects:id {:params {:project/id project-id}}))

(defn teams#res:fetch-one [sys team-id]
  (pages/res:fetch sys :routes.api/teams:id {:params {:team/id team-id}}))
