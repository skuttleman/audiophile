(ns com.ben-allred.audiophile.common.core.ui-components.audio
  (:require
    [com.ben-allred.audiophile.common.core.navigation.core :as nav]
    [com.ben-allred.audiophile.common.core.resources.core :as res]
    [com.ben-allred.audiophile.common.core.resources.protocols :as pres]
    [com.ben-allred.audiophile.common.core.stubs.reagent :as r]
    [com.ben-allred.audiophile.common.core.resources.http :as http]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.common.core.ui-components.core :as comp]
    [com.ben-allred.audiophile.common.core.ui-components.input-fields :as in]
    [com.ben-allred.vow.core :as v #?@(:cljs [:include-macros true])]))

(defn ^:private on-load [blob state id]
  #?(:cljs
     (doto (js/WaveSurfer.create #js {:container         (str "#" id)
                                      :waveColor         "lightblue"
                                      :closeAudioContext true
                                      :progressColor     "blue"})
       (.loadBlob blob)
       (.on "ready" (fn [_]
                      (swap! state assoc :ready? true))))))

(defn player [{:keys [*artifact]}]
  (fn [artifact-id]
    #?(:cljs
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
              (some-> @state :surfer .destroy))})))))

(deftype ArtifactResource [http-client nav]
  pres/IResource
  (request! [_ opts]
    (http/get http-client
              (nav/path-for nav :api/artifact {:route-params opts})
              {:response-type :blob})))

(defn resource [{:keys [http-client nav]}]
  (->ArtifactResource http-client nav))
