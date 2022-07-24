(ns audiophile.ui.views.file.core
  (:require
    [audiophile.common.core.utils.logger :as log]
    [audiophile.common.core.utils.maps :as maps]
    [audiophile.common.infrastructure.resources.core :as res]
    [audiophile.ui.components.core :as comp]
    [audiophile.ui.components.input-fields.dropdown :as dd]
    [audiophile.ui.forms.core :as forms]
    [audiophile.ui.views.common.core :as views]
    [audiophile.ui.views.file.audio :as audio]
    [audiophile.ui.views.file.services :as serv]
    [reagent.core :as r]))

(defn ^:private version-name [version]
  [:span (:file-version/name version)])

(defn ^:private attrs->content [{:keys [options-by-id value]}]
  (get-in options-by-id [(first value) :file-version/name]))

(defn ^:private page* [sys {:keys [*file]} file current-version-id selected-version-id]
  (r/with-let [versions (map (juxt :file-version/id identity) (:file/versions file))
               *form (serv/versions#form:selector sys selected-version-id)
               *select (serv/versions#res:set-active sys *file (:file/id file))
               versions-by-id (into {} versions)
               modal-attrs {:on-success (comp (serv/versions#nav:qp sys)
                                              :file-version/id)
                            :*res       *file
                            :file       file}
               click (serv/files#modal:version sys [::views/version modal-attrs])]
    (let [{:keys [file-version-id]} @*form
          version (get versions-by-id file-version-id)
          artifact-id (:file-version/artifact-id version)
          attrs {:artifact-id     artifact-id
                 :file-id         (:file/id file)
                 :file-version-id file-version-id}]
      (cond
        (res/requesting? *select)
        [comp/spinner]

        artifact-id
        [:div.panel
         [:div.panel-heading
          [:div.layout--align-center
           [:a.link.layout--space-after {:href (serv/projects#nav:ui sys (:file/project-id file))}
            [comp/icon :hand-point-left]]
           (:file/name file)
           [:div.layout--inset
            (if (empty? (rest versions))
              [:small "version: " [version-name version]]
              [dd/dropdown (-> {:attrs->content attrs->content
                                :force-value?   true
                                :item-control   version-name
                                :options        versions
                                :options-by-id  versions-by-id}
                               (forms/with-attrs *form [:file-version-id])
                               dd/singleable)])]
           [:div.buttons
            [comp/plain-button
             {:class    ["is-outlined" "is-info"]
              :on-click click}
             "New version"]
            (when-not (= file-version-id current-version-id)
              [comp/plain-button
               {:class    ["is-outlined" "is-primary"]
                :on-click (fn [_]
                            (res/request! *select {:file-version/id file-version-id}))}
               "Set as current"])]]]
         ^{:key artifact-id} [audio/player sys attrs]]

        :else
        [comp/alert :error "File version could not be found"]))
    (finally
      (forms/destroy! *form)
      (res/destroy! *select))))

(defn ^:private init [{:keys [nav] :as sys} attrs file]
  (let [route @nav
        current-version-id (-> file :file/versions first :file-version/id)
        selected-version-id (or (-> route :params :file-version-id)
                                (serv/files#nav:add-version! sys route current-version-id))]
    [page* sys attrs file current-version-id selected-version-id]))

(defn ^:private page [{:keys [nav] :as sys}]
  (r/with-let [file-id (-> @nav :params :file/id)
               *file (serv/files#res:fetch-one sys file-id)]
    [comp/with-resource *file [init sys (maps/->m *file)]]
    (finally
      (res/destroy! *file))))

(defn root [sys]
  [page sys])
