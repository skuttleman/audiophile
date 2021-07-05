(ns com.ben-allred.audiophile.ui.api.components.player
  (:require
    [com.ben-allred.audiophile.common.api.navigation.core :as nav]
    [com.ben-allred.audiophile.common.core.resources.core :as res]
    [com.ben-allred.audiophile.common.core.resources.protocols :as pres]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.common.infrastructure.http.core :as http]
    [com.ben-allred.audiophile.ui.core.components.core :as comp]
    [com.ben-allred.audiophile.ui.core.components.protocols :as pcomp]
    [com.ben-allred.audiophile.ui.core.utils.reagent :as r]
    [com.ben-allred.vow.core :as v :include-macros true]))

(defn ^:private on-load ^js/WaveSurfer [player id blob]
  (let [regions (js/WaveSurfer.regions.create #js {:regionsMinLength 2
                                                   :dragSelection    #js {:slop 5}})]
    (doto (js/WaveSurfer.create #js {:container         (str "#" id)
                                     :waveColor         "lightblue"
                                     :closeAudioContext true
                                     :progressColor     "blue"
                                     :plugins           #js [regions]})
      (.loadBlob blob)
      (.on "region-created" (fn [_]
                              (comp/set-region! player)))
      (.on "region-update-end" (fn [^js/Object e]
                                 (comp/set-region! player {:start (.-start e)
                                                           :end   (.-end e)}))))))

(deftype ArtifactPlayer [id state *artifact]
  pcomp/IIdentify
  (id [_]
    id)

  pcomp/ISelectRegion
  (set-region! [this opts]
    (when-let [^js/WaveSurfer surfer (when (comp/ready? this)
                                       (:surfer @state))]
      (.clearRegions surfer)
      (if-let [{:keys [start end]} opts]
        (do (.addRegion surfer #js {:start start :end end})
            (swap! state assoc :region [start end])
            (.seekTo surfer (/ start (.getDuration surfer))))
        (swap! state dissoc :region))))
  (region [_]
    (:region @state))

  pcomp/ILoad
  (load! [this {:keys [artifact-id on-change]}]
    (remove-watch state ::events)
    (when on-change
      (add-watch state ::events (fn [_ _ old new]
                                  (let [data (select-keys new #{:region :position})]
                                    (when (not= data (select-keys old #{:region :position}))
                                      (on-change data))))))
    (-> *artifact
        (res/request! {:artifact-id artifact-id})
        (v/then (fn [blob]
                  (let [^js/Object surfer (on-load this id blob)
                        update-position (fn [_]
                                          (swap! state assoc :position (.getCurrentTime surfer)))]
                    (swap! state assoc :surfer surfer :position 0)
                    (doto surfer
                      (.on "ready" (fn [_]
                                     (swap! state assoc :ready? true)))
                      (.on "seek" update-position)
                      (.on "finish" update-position)
                      (.on "pause" update-position)))))))
  (ready? [_]
    (:ready? @state))
  (destroy! [_]
    (when-let [^js/WaveSurfer surfer (:surfer @state)]
      (.destroy surfer)))

  pcomp/IPlayer
  (play-pause! [this]
    (when-let [^js/WaveSurfer surfer (when (comp/ready? this)
                                       (:surfer @state))]
      (.playPause surfer)))
  (playing? [this]
    (when-let [^js/WaveSurfer surfer (when (comp/ready? this)
                                       (:surfer @state))]
      (.isPlaying surfer))))

(defn artifact-player [{:keys [*artifact]}]
  (->ArtifactPlayer (name (gensym)) (r/atom nil) *artifact))

(deftype ArtifactResource [http-client nav]
  pres/IResource
  (request! [_ opts]
    (http/get http-client
              (nav/path-for nav :api/artifact {:route-params opts})
              {:response-type :blob})))

(defn res-artifact [{:keys [http-client nav]}]
  (->ArtifactResource http-client nav))
