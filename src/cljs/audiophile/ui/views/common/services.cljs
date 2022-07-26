(ns audiophile.ui.views.common.services
  (:require
    [audiophile.common.domain.validations.core :as val]
    [audiophile.common.domain.validations.specs :as specs]
    [audiophile.common.infrastructure.navigation.core :as nav]
    [audiophile.common.infrastructure.resources.core :as res]
    [audiophile.ui.forms.standard :as form.std]
    [audiophile.ui.resources.impl :as ires]
    [audiophile.ui.services.pages :as pages]
    [com.ben-allred.vow.core :as v :include-macros true]))

(def ^:private ui-version-spec
  ^{:missing-keys {:artifact/details "uploaded file is required"}}
  [:map
   [:artifact/details
    [:fn (fn [{:artifact/keys [id]}]
           (uuid? id))]]
   [:file-version/name specs/trimmed-string?]])

(def ^:private files#validator:version
  (val/validator {:spec ui-version-spec}))

(def ^:private projects#validator:new
  (val/validator {:spec specs/project:create}))

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

(defn files#local->remote [data]
  (-> data
      (select-keys #{:file/name :file-version/name})
      (assoc :artifact/id (-> data :artifact/details :artifact/id))))

(defn files#form:version [{:keys [store] :as sys} attrs file-id]
  (let [*form (form.std/create store nil files#validator:version)
        attrs (assoc attrs :local->remote files#local->remote)]
    (pages/form:new sys attrs *form :routes.api/files:id.versions {:params {:file/id file-id}})))

(defn modals#with-on-success [{:keys [*res close!] :as attrs}]
  (update attrs :on-success (fn [on-success]
                              (fn [result]
                                (v/and (v/resolve)
                                       (when close!
                                         (close! result))
                                       (some-> *res res/request!)
                                       (when on-success
                                         (on-success result)))))))

(defn projects#form:new [{:keys [store] :as sys} attrs team-id]
  (let [*form (form.std/create store {:project/team-id team-id} projects#validator:new)]
    (pages/form:new sys attrs *form :routes.api/projects)))

(defn projects#modal:create [sys body]
  (pages/modal:open sys [:h1.subtitle "Create a project"] body))

(def teams#type->icon
  {:PERSONAL      ["Personal Team" :user]
   :COLLABORATIVE ["Collaborative Team" :users]})
