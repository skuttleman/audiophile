(ns com.ben-allred.audiophile.common.infrastructure.views.components.audio
  (:require
    [com.ben-allred.audiophile.common.app.navigation.core :as nav]
    [com.ben-allred.audiophile.common.app.resources.core :as res]
    [com.ben-allred.audiophile.common.app.resources.protocols :as pres]
    [com.ben-allred.audiophile.common.core.stubs.reagent :as r]
    [com.ben-allred.audiophile.common.app.http :as http]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.common.infrastructure.views.components.core :as comp]
    [com.ben-allred.audiophile.common.infrastructure.views.components.input-fields :as in]
    [com.ben-allred.vow.core :as v #?@(:cljs [:include-macros true])]))

(defn ^:private on-load [blob id]
  #?(:cljs
     (doto (js/WaveSurfer.create #js {:container         (str "#" id)
                                      :waveColor         "lightgrey"
                                      :closeAudioContext true
                                      :progressColor     "blue"})
       (.loadBlob blob))))

(defn player [{:keys [*artifact]}]
  (fn [artifact-id]
    #?(:cljs
       (let [id (name (gensym))
             state (r/atom nil)]
         (-> *artifact
             (res/request! {:artifact-id artifact-id})
             (v/then-> (on-load id)
                       (->> (swap! state assoc :surfer))))
         (r/create-class
           {:reagent-render
            (fn [_artifact-id]
              [:div {:style {:width "100%"}}
               [:div.audio-player {:id id}]
               (when-let [{:keys [playing? surfer]} @state]
                 [:div.buttons
                  [in/plain-button {:on-click (fn [_]
                                                (swap! state update :playing? not)
                                                (.playPause ^js/WaveSurfer surfer))}
                   [comp/icon (if playing? :pause :play)]]])])
            :component-will-unmount
            (fn [_]
              (some-> @state :surfer .destroy))})))))

(deftype ArtifactResource [http-client nav]
  pres/IResource
  (request! [_ opts]
    (http/get http-client
              (nav/path-for nav :api/artifact {:route-params opts})
              {:response-type :blob})))

(defn resource [{:keys [http-client nav]}]
  (->ArtifactResource http-client nav))
