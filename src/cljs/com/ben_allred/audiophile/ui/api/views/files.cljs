(ns com.ben-allred.audiophile.ui.api.views.files
  (:require
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.common.core.utils.strings :as strings]
    [com.ben-allred.audiophile.ui.api.views.core :as views]
    [com.ben-allred.audiophile.ui.core.components.core :as comp]
    [com.ben-allred.audiophile.ui.core.components.input-fields :as in]
    [com.ben-allred.audiophile.ui.core.components.input-fields.dropdown :as dd]
    [com.ben-allred.audiophile.ui.core.forms.core :as forms]))

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

(defn ^:private comment-viewer [comments]
  [:div "comments here: " (count comments)])

(defn comment-label [*form]
  (let [[start end] (:comment/selection @*form)]
    [:div.layout--space-between
     {:style {:align-items :center}}
     [:span "comment"]
     [:div {:style {:margin-right "-10px"}}
      [in/checkbox (forms/with-attrs {:label            (->selection-label start end)
                                      :form-field-class ["inline"]}
                                     *form
                                     [:comment/with-selection?])]]]))

(defn ^:private player* [{:keys [artifact-id file-id file-version-id]} *int *comments player]
  (let [*form (views/comment-form *int file-id file-version-id)]
    (fn [_attrs _*int _*comments _player]
      [:div.panel-block.layout--stack-between
       [player
        (-> *form
            (forms/with-attrs [:comment/selection])
            (update :on-change comp ->selection))
        artifact-id]
       [comp/form {:*form        *form
                   :style        {:min-width "300px"}
                   :on-submitted (views/on-comment-created *int)}
        [in/textarea (forms/with-attrs {:label       [comment-label *form]
                                        :auto-focus? true}
                                       *form
                                       [:comment/body])]]
       [comp/with-resource *comments comment-viewer]])))

(defn ^:private one* [*int *comments file file-version-id _player]
  (let [versions (map (juxt :file-version/id identity) (:file/versions file))
        *form (views/qp-form *int {:file-version-id file-version-id
                                   :file/id         (:file/id file)})
        versions-by-id (into {} versions)]
    (fn [_*int _*comments file _file-version-id player]
      (let [{:keys [file-version-id]} @*form
            version (get versions-by-id file-version-id)
            artifact-id (:file-version/artifact-id version)
            attrs {:artifact-id     artifact-id
                   :file-id         (:file/id file)
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
         ^{:key artifact-id} [player* attrs *int *comments player]]))))

(defn ^:private init* [file *int *comments {:keys [params]} player]
  (let [file-id (:file/id params)]
    (if-let [file-version-id (:file-version-id params)]
      ^{:key file-id} [one* *int *comments file file-version-id player]
      (views/update-qp! *int {:file-version-id (:file-version/id (first (:file/versions file)))}))))

(defn one [{:keys [*comments *file *int player]}]
  (fn [{:nav/keys [route]}]
    (let [opts {:nav/params route}]
      [comp/with-resource [*file opts] init* *int [*comments opts] route player])))
