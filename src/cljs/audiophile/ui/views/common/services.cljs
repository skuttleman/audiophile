(ns audiophile.ui.views.common.services
  (:require
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

(def ^:private files#validator:version
  (val/validator {:spec ui-version-spec}))

(defn files#local->remove [data]
  (-> data
      (select-keys #{:file/name :version/name})
      (assoc :artifact/id (-> data :artifact/details :artifact/id))))

(defn artifacts#res:new [{:keys [http-client nav store]}]
  (ires/http store
             http-client
             (fn [{:keys [files] :as opts}]
               (-> {:method           :post
                    :url              (nav/path-for nav :routes.api/artifact)
                    :http/async?      true
                    :multipart-params (for [file files]
                                        ["files[]" file])}
                   (merge (select-keys opts #{:multi? :on-progress}))))))

(defn files#form:version [{:keys [store] :as sys} attrs file-id]
  (let [*form (form.std/create store nil files#validator:version)
        attrs (assoc attrs :local->remote files#local->remove)]
    (pages/form:new sys attrs *form :routes.api/files:id {:params {:file/id file-id}})))
