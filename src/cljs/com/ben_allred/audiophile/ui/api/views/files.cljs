(ns com.ben-allred.audiophile.ui.api.views.files
  (:require
    [com.ben-allred.audiophile.common.core.resources.protocols :as pres]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.common.core.utils.strings :as strings]
    [com.ben-allred.audiophile.ui.api.forms.query-params :as form.qp]
    [com.ben-allred.audiophile.ui.api.forms.standard :as form]
    [com.ben-allred.audiophile.ui.api.forms.submittable :as form.sub]
    [com.ben-allred.audiophile.ui.core.components.core :as comp]
    [com.ben-allred.audiophile.ui.core.components.input-fields :as in]
    [com.ben-allred.audiophile.ui.core.components.input-fields.dropdown :as dd]
    [com.ben-allred.audiophile.ui.core.forms.core :as forms]
    [com.ben-allred.vow.core :as v :include-macros true]
    [com.ben-allred.audiophile.common.core.resources.core :as res]))

(defn ^:private version-name [version]
  [:span (:file-version/name version)])

(defn ^:private attrs->content [{:keys [options-by-id value]}]
  (get-in options-by-id [(first value) :file-version/name]))

(defn ^:private ->time [sec]
  (let [min (js/Math.floor (/ sec 60))
        sec (js/Math.floor (- sec (* min 60)))]
    (strings/format "%d:%02d" min sec)))

(defn ^:private ->selection-label [start end]
  (cond-> (str "@" (->time start))
    (not= start end) (str " - " (->time end))))

(defn ^:private ->selection [{:keys [position region]}]
  (cond
    region region
    position [position position]
    :else [0 0]))

(defn ^:private comment-viewer [comments attrs *comment player]
  [:div "comments here: " (count comments)])

(defn comment-label [*form]
  (let [[start end] (:comment/selection @*form)]
    [:div.layout--space-between
     {:style {:align-items :center}}
     [:span "comment"]
     [in/checkbox (forms/with-attrs {:label            (->selection-label start end)
                                     :form-field-class ["inline"]}
                                    *form
                                    [:comment/with-selection?])]]))

(defn ^:private player* [{:keys [artifact-id file-id file-version-id]} _*comments *comment player]
  (let [*form (form.sub/create *comment
                               (form/create {:comment/file-version-id file-version-id
                                             :comment/with-selection? true
                                             :comment/selection       [0 0]
                                             :file/id                 file-id}
                                            (constantly nil)))]
    (fn [attrs *comments _*comment _player]
      [:div.panel-block.layout--stack-between
       [player
        (-> *form
            (forms/with-attrs [:comment/selection])
            (update :on-change comp ->selection))
        artifact-id]
       [comp/form {:*form *form
                   :style {:min-width "300px"}}
        [in/textarea (forms/with-attrs {:label       [comment-label *form]
                                        :auto-focus? true}
                                       *form
                                       [:comment/body])]]
       [comp/with-resource *comments comment-viewer attrs player]])))

(defn ^:private one* [file _*comments *qp *comment _file-id file-version-id _player]
  (let [versions (map (juxt :file-version/id identity) (:file/versions file))
        *form (-> {:file-version-id file-version-id}
                  form/create
                  (form.qp/create *qp))
        versions-by-id (into {} versions)]
    (fn [file *comments _*qp _*comment file-id _file-version-id player]
      (let [{:keys [file-version-id]} @*form
            version (get versions-by-id file-version-id)
            artifact-id (:file-version/artifact-id version)
            attrs {:artifact-id     artifact-id
                   :file-id         file-id
                   :file-version-id file-version-id}]
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
         ^{:key artifact-id} [player* attrs *comments *comment player]]))))

(defn init* [file *comments {route :nav/params :as opts} *qp *comment player]
  (let [file-id (get-in route [:route-params :file-id])]
    (if-let [file-version-id (get-in route [:query-params :file-version-id])]
      ^{:key file-id} [one* file [*comments opts] *qp *comment file-id file-version-id player]
      (forms/update-qp! *qp assoc :file-version-id (:file-version/id (first (:file/versions file)))))))

(defn one [{:keys [*comment *comments *file *qp player]}]
  (fn [{:nav/keys [route]}]
    (let [opts {:nav/params route}]
      [comp/with-resource [*file opts] init* *comments opts *qp *comment player])))
