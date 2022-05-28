(ns audiophile.ui.views.file.audio
  (:require
    [audiophile.common.core.utils.logger :as log]
    [audiophile.common.core.utils.strings :as strings]
    [audiophile.common.infrastructure.resources.core :as res]
    [audiophile.ui.components.core :as comp]
    [audiophile.ui.components.input-fields :as in]
    [audiophile.ui.forms.core :as forms]
    [audiophile.ui.views.file.services :as serv]
    [reagent.core :as r]))

(defn ^:private ->time [sec]
  (let [min (js/Math.floor (/ sec 60))
        sec (js/Math.floor (- sec (* min 60)))]
    (strings/format "%d:%02d" min sec)))

(defn ^:private ->selection [{:keys [position region]}]
  (or region
      (when position [position position])
      [0 0]))

(defn ^:private ->selection-label [start end]
  (cond-> (str "@" (->time start))
    (not= start end) (str " - " (->time end))))

(defn ^:private comment-viewer [sys *player comment]
  [:p (:comment/body comment)])

(defn ^:private comments-viewer [sys *player comments]
  [:ul
   (for [[idx [comm :as comments]] (->> comments
                                        reverse
                                        (sequence (comp (partition-by (comp :commenter/id :comment/commenter))
                                                        (map-indexed vector))))]
     ^{:key idx}
     [:li
      [:em "@" (-> comm :comment/commenter :commenter/handle)]
      [:ul
       (for [comment comments]
         ^{:key (:comment/id comment)}
         [:li.layout--inset
          [comment-viewer sys *player comment]])]])])

(defn comment-label [*form]
  (let [[start end] (:comment/selection @*form)]
    [:div.layout--space-between
     {:style {:align-items :center}}
     [:span "Comment"]
     [:div {:style {:margin-right "-10px"}}
      [in/checkbox (forms/with-attrs {:label            (->selection-label start end)
                                      :form-field-class ["inline"]}
                                     *form
                                     [:comment/with-selection?])]]]))

(defn audio [*player]
  (let [ready? (serv/ready? *player)
        error? (serv/error? *player)]
    [:div {:style {:width "100%"}}
     (cond
       error? [comp/alert :error "The audio file could not be loaded."]
       (not ready?) [comp/spinner {:size :large}])
     [:div.audio-player {:id (serv/id *player)}]
     (when ready?
       [:div.buttons
        [comp/plain-button {:on-click (fn [_]
                                        (serv/play-pause! *player))
                            :disabled (not ready?)}
         [comp/icon (if (serv/playing? *player) :pause :play)]]
        (when (serv/region *player)
          [comp/plain-button {:on-click (fn [_]
                                          (serv/set-region! *player))}
           "clear region"])
        (when-not ready?
          [comp/spinner])])]))

(defn player [sys {:keys [artifact-id file-id file-version-id]}]
  (r/with-let [*comments (serv/comments#res:fetch-all sys file-id)
               *form (serv/comments#form:new sys *comments file-version-id)
               *artifact (serv/artifacts#res:fetch-one sys)
               *player (serv/player#create *artifact (-> {:artifact-id artifact-id}
                                                         (forms/with-attrs *form [:comment/selection])
                                                         (update :on-change comp ->selection)))]
    [:div.panel-block.layout--stack-between
     [audio *player]
     [:div {:style {:min-width "300px"
                    :max-width "700px"
                    :width     "50%"}}
      [comp/form {:*form *form}
       [in/textarea (forms/with-attrs {:label       [comment-label *form]
                                       :auto-focus? true}
                                      *form
                                      [:comment/body])]]
      [comp/with-resource [*comments {:file-version-id file-version-id}] [comments-viewer sys *player]]]]
    (finally
      (res/destroy! *artifact)
      (serv/destroy! *player)
      (forms/destroy! *form)
      (res/destroy! *comments))))
