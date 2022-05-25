(ns audiophile.ui.views.project.services
  (:require
    [audiophile.common.core.utils.logger :as log]
    [audiophile.common.domain.validations.core :as val]
    [audiophile.common.domain.validations.specs :as specs]
    [audiophile.common.infrastructure.navigation.core :as nav]
    [audiophile.ui.forms.standard :as form.std]
    [audiophile.ui.resources.impl :as ires]
    [audiophile.ui.services.pages :as pages]))

(def ^:private ui-version-spec
  ^{:missing-keys {:artifact/details "uploaded file is required"}}
  [:map
   [:artifact/details
    [:fn (fn [{:artifact/keys [id]}]
           (uuid? id))]]
   [:version/name specs/trimmed-string?]])

(def ^:private ui-file-spec
  (conj ui-version-spec [:file/name specs/trimmed-string?]))

(def ^:private files#validator:new
  (val/validator {:spec ui-file-spec}))

(def ^:private files#validator:version
  (val/validator {:spec ui-version-spec}))

(defn ^:private files#local->remove [data]
  (-> data
      (select-keys #{:file/name :version/name})
      (assoc :artifact/id (-> data :artifact/details :artifact/id))))

(defn artifacts#res:new [{:keys [http-client nav]}]
  (ires/http http-client
             (fn [{:keys [files] :as opts}]
               (-> {:method           :post
                    :url              (nav/path-for nav :routes.api/artifact)
                    :http/async?      true
                    :multipart-params (for [file files]
                                        ["files[]" file])}
                   (merge (select-keys opts #{:multi? :on-progress}))))))

(defn files#form:new [sys attrs project-id]
  (let [*form (form.std/create nil files#validator:new)
        attrs (assoc attrs :local->remote files#local->remove)]
    (pages/form:new sys attrs *form :routes.api/projects:id.files {:params {:project/id project-id}})))

(defn files#form:version [sys attrs file-id]
  (let [*form (form.std/create nil files#validator:version)
        attrs (assoc attrs :local->remote files#local->remove)]
    (pages/form:new sys attrs *form :routes.api/files:id {:params {:file/id file-id}})))

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
