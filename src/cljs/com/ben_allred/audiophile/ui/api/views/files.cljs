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
    [com.ben-allred.vow.core :as v]))

(defn ^:private version-name [version]
  [:span (:file-version/name version)])

(defn ^:private attrs->content [{:keys [options-by-id value]}]
  (get-in options-by-id [(first value) :file-version/name]))

(defn ^:private ->time [sec]
  (let [min (js/Math.floor (/ sec 60))
        sec (js/Math.floor (- sec (* min 60)))]
    (strings/format "%d:%02d" min sec)))

(defn ^:private ->selection-label [start end]
  (if (= start end)
    (->time start)
    (str (->time start) " - " (->time end))))

(defn ^:private ->selection [{:keys [position region]}]
  (cond
    region region
    position [position position]))

(defn ^:private player* [{:keys [artifact-id file-version-id]} player]
  (let [*form (form.sub/create (reify pres/IResource
                                 (request! [_ opts]
                                   (v/resolve (:form/value opts)))
                                 (status [_]
                                   :init))
                               (form/create {:comment/file-version-id file-version-id
                                             :comment/with-selection? true}
                                            (constantly nil)))]
    (fn [_attrs _player]
      [:div.panel-block.layout--stack-between
       [player
        (-> *form
            (forms/with-attrs [:comment/selection])
            (update :on-change comp ->selection))
        artifact-id]
       [comp/form {:*form        *form
                   :on-submitted (fn [vow]
                                   (v/peek vow
                                           (fn [_]

                                             (log/warn "SUBMITTED" _))
                                           nil))}
        [log/pprint @*form]
        (when-let [[start end] (:comment/selection @*form)]
          [in/checkbox (forms/with-attrs {:label            (->selection-label start end)
                                          :form-field-class ["inline"]}
                                         *form
                                         [:comment/with-selection?])])
        [in/textarea (forms/with-attrs {:label       "comment"
                                        :auto-focus? true}
                                       *form
                                       [:comment/body])]]])))

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
         ^{:key artifact-id} [player*
                              {:artifact-id     artifact-id
                               :file-version-id file-version-id}
                              player]]))))

(defn init* [file route *qp player]
  (if-let [file-version-id (get-in route [:query-params :file-version-id])]
    [one* file *qp file-version-id player]
    (forms/update-qp! *qp assoc :file-version-id (:file-version/id (first (:file/versions file))))))

(defn one [{:keys [*file player *qp]}]
  (fn [state]
    [comp/with-resource [*file {:nav/params (:nav/route state)}] init* (:nav/route state) *qp player]))
