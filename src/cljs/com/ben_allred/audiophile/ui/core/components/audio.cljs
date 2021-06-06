(ns com.ben-allred.audiophile.ui.core.components.audio
  (:require
    [com.ben-allred.audiophile.common.core.resources.core :as res]
    [com.ben-allred.audiophile.ui.core.utils.reagent :as r]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.ui.core.components.core :as comp]
    [com.ben-allred.audiophile.ui.core.components.input-fields :as in]
    [com.ben-allred.vow.core :as v :include-macros true]))

(defn ^:private on-load [blob state id]
  (doto (js/WaveSurfer.create #js {:container         (str "#" id)
                                   :waveColor         "lightblue"
                                   :closeAudioContext true
                                   :progressColor     "blue"})
    (.loadBlob blob)
    (.on "ready" (fn [_]
                   (swap! state assoc :ready? true)))))

(defn player [{:keys [*artifact]}]
  (fn [artifact-id]
    (let [id (name (gensym))
          state (r/atom nil)]
      (-> *artifact
          (res/request! {:artifact-id artifact-id})
          (v/then-> (on-load state id)
                    (->> (swap! state assoc :surfer))))
      (r/create-class
        {:reagent-render
         (fn [_artifact-id]
           (let [{:keys [playing? surfer ready?] :as st} @state]
             [:div {:style {:width "100%"}}
              (when-not st
                [comp/spinner {:size :large}])
              [:div.audio-player {:id id}]
              (when st
                [:div.buttons
                 [in/plain-button {:on-click (fn [_]
                                               (swap! state update :playing? not)
                                               (.playPause ^js/WaveSurfer surfer))
                                   :disabled (not ready?)}
                  [comp/icon (if playing? :pause :play)]]
                 (when-not ready?
                   [comp/spinner])])]))
         :component-will-unmount
         (fn [_]
           (some-> @state :surfer .destroy))}))))
