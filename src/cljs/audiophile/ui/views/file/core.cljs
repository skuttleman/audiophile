(ns audiophile.ui.views.file.core
  (:require
    [audiophile.common.core.utils.logger :as log]
    [audiophile.common.infrastructure.resources.core :as res]
    [audiophile.ui.components.core :as comp]
    [audiophile.ui.components.input-fields.dropdown :as dd]
    [audiophile.ui.forms.core :as forms]
    [audiophile.ui.store.queries :as q]
    [audiophile.ui.views.file.audio :as audio]
    [audiophile.ui.views.file.services :as serv]
    [reagent.core :as r]))

(defn ^:private version-name [version]
  [:span (:file-version/name version)])

(defn ^:private attrs->content [{:keys [options-by-id value]}]
  (get-in options-by-id [(first value) :file-version/name]))

(defn ^:private page* [sys file initial-version-id]
  (r/with-let [versions (map (juxt :file-version/id identity) (:file/versions file))
               *form (serv/versions#form:selector sys initial-version-id)
               versions-by-id (into {} versions)]
    (let [{:keys [file-version-id]} @*form
          version (get versions-by-id file-version-id)
          artifact-id (:file-version/artifact-id version)
          attrs {:artifact-id     artifact-id
                 :file-id         (:file/id file)
                 :file-version-id file-version-id}]
      (if artifact-id
        [:div.panel
         [:div.panel-heading
          [:div.layout--align-center
           [:span (:file/name file)]
           [:div.layout--inset
            (if (empty? (rest versions))
              [:small "version: " [version-name version]]
              [dd/dropdown (-> {:attrs->content attrs->content
                                :force-value?   true
                                :item-control   version-name
                                :options        versions
                                :options-by-id  versions-by-id}
                               (forms/with-attrs *form [:file-version-id])
                               dd/singleable)])]]]
         ^{:key artifact-id} [audio/player sys attrs]]
        [comp/alert :error "File version could not be found"]))
    (finally
      (forms/destroy! *form))))

(defn ^:private init [file {:keys [nav] :as sys}]
  (let [route @nav
        version-id (or (-> route :params :file-version-id)
                       (serv/files#nav:add-version! sys route file))]
    [page* sys file version-id]))

(defn ^:private page [{:keys [nav] :as sys}]
  (r/with-let [file-id (-> @nav :params :file/id)
               *file (serv/files#res:fetch-one sys file-id)]
    [comp/with-resource *file init sys]
    (finally
      (res/destroy! *file))))

(defn root [sys]
  [page sys])
