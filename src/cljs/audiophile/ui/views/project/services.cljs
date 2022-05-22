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
                    :url              (nav/path-for nav :api/artifacts)
                    :http/async?      true
                    :multipart-params (for [file files]
                                        ["files[]" file])}
                   (merge (select-keys opts #{:multi? :on-progress}))))))

(defn files#form:new [sys attrs project-id]
  (let [*form (form.std/create nil files#validator:new)
        attrs (assoc attrs :local->remote files#local->remove)]
    (pages/form:new sys attrs *form :api/project.files {:params {:project/id project-id}})))

(defn files#form:version [sys attrs file-id]
  (let [*form (form.std/create nil files#validator:version)
        attrs (assoc attrs :local->remote files#local->remove)]
    (pages/form:new sys attrs *form :api/file {:params {:file/id file-id}})))

(defn files#res:fetch-all [sys project-id]
  (pages/res:fetch sys :api/project.files {:params {:project/id project-id}}))

(defn projects#res:fetch-one [sys project-id]
  (pages/res:fetch sys :api/project {:params {:project/id project-id}}))

(defn teams#res:fetch-one [sys team-id]
  (pages/res:fetch sys :api/team {:params {:team/id team-id}}))
