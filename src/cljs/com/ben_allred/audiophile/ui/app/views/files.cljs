(ns com.ben-allred.audiophile.ui.app.views.files
  (:require
    [com.ben-allred.audiophile.ui.app.forms.query-params :as form.qp]
    [com.ben-allred.audiophile.ui.app.forms.standard :as form]
    [com.ben-allred.audiophile.ui.core.forms.core :as forms]
    [com.ben-allred.audiophile.ui.core.components.core :as comp]
    [com.ben-allred.audiophile.ui.core.components.input-fields.dropdown :as dd]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]))

(defn ^:private version-name [version]
  [:span (:file-version/name version)])

(defn ^:private attrs->content [{:keys [options-by-id value]}]
  (get-in options-by-id [(first value) :file-version/name]))

(defn ^:private one* [file *qp file-version-id _player]
  (let [versions (map (juxt :file-version/id identity) (:file/versions file))
        *form (-> {:file-version-id file-version-id}
                  form/create
                  (form.qp/create *qp))
        versions-by-id (into {} versions)]
    (fn [file _*qp _file-version-id player]
      (let [{:keys [file-version-id]} @*form
            version (get versions-by-id file-version-id)
            artifact-id (:file-version/artifact-id version)]
        [:div.panel
         [:div.panel-heading
          [:div.layout--align-center
           [:span (:file/name file)]
           [:div.layout--inset
            [dd/dropdown (-> {:attrs->content attrs->content
                              :force-value?   true
                              :item-control   version-name
                              :options        versions
                              :options-by-id  versions-by-id}
                             (forms/with-attrs *form [:file-version-id])
                             dd/singleable)]]]]
         [:div.panel-block
          ^{:key artifact-id} [player artifact-id]]]))))

(defn init* [file route *qp player]
  (if-let [file-version-id (get-in route [:query-params :file-version-id])]
    [one* file *qp file-version-id player]
    (forms/update-qp! *qp assoc :file-version-id (:file-version/id (first (:file/versions file))))))

(defn one [{:keys [*file player *qp]}]
  (fn [state]
    [comp/with-resource [*file {:nav/params (:nav/route state)}] init* (:nav/route state) *qp player]))
