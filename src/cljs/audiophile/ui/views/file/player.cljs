(ns audiophile.ui.views.file.player
  (:require
    [audiophile.common.infrastructure.resources.core :as res]
    [audiophile.common.infrastructure.protocols :as pcom]
    [audiophile.ui.views.file.protocols :as proto]
    [com.ben-allred.vow.core :as v]))

(defn ^:private on-load ^js/WaveSurfer [player id]
  (fn [blob]
    (let [regions (js/WaveSurfer.regions.create #js {:regionsMinLength 2
                                                     :dragSelection    #js {:slop 5}})]
      (doto (js/WaveSurfer.create #js {:container         (str "#" id)
                                       :waveColor         "lightblue"
                                       :closeAudioContext true
                                       :progressColor     "blue"
                                       :plugins           #js [regions]})
        (.loadBlob blob)
        (.on "region-created" (fn [_]
                                (proto/set-region! player nil)))
        (.on "region-update-end" (fn [^js/Object e]
                                   (proto/set-region! player {:start (.-start e)
                                                              :end   (.-end e)})))))))

(defn ^:private init-surfer [state]
  (fn [^js/WaveSurfer surfer]
    (let [update-position (fn [_]
                            (swap! state assoc :position (.getCurrentTime surfer)))]
      (swap! state assoc :surfer surfer :position 0)
      (doto surfer
        (.on "ready" (fn [_]
                       (swap! state assoc :ready? true)))
        (.on "seek" update-position)
        (.on "finish" update-position)
        (.on "pause" update-position)))))

(defn ^:private artifact-player#destroy! [state]
  (when-let [^js/WaveSurfer surfer (:surfer @state)]
    (.destroy surfer)
    (swap! state dissoc :surfer :position)))

(defn ^:private artifact-player#set-region! [this state {:keys [start end]}]
  (when-let [^js/WaveSurfer surfer (when (proto/ready? this)
                                     (:surfer @state))]
    (.clearRegions surfer)
    (if (and start end)
      (do (.addRegion surfer #js {:start start :end end})
          (swap! state assoc :region [start end]))
      (swap! state dissoc :region))))

(defn artifact-player#load! [this id state *artifact {:keys [artifact-id on-change]}]
  (remove-watch state ::events)
  (swap! state dissoc :ready? :error?)
  (when on-change
    (add-watch state ::events (fn [_ _ old new]
                                (let [data (select-keys new #{:region :position})]
                                  (when (not= data (select-keys old #{:region :position}))
                                    (on-change data))))))
  (-> *artifact
      (res/request! {:artifact/id artifact-id})
      (v/then (comp (init-surfer state) (on-load this id))
              (fn [_]
                (swap! state assoc :error? true)))))

(defn ^:private artifact-player#play-pause! [this state]
  (when-let [^js/WaveSurfer surfer (when (proto/ready? this)
                                     (:surfer @state))]
    (.playPause surfer)))

(defn ^:private artifact-player#playing? [this state]
  (when-let [^js/WaveSurfer surfer (when (proto/ready? this)
                                     (:surfer @state))]
    (.isPlaying surfer)))

(deftype ArtifactPlayer [id state *artifact]
  pcom/IIdentify
  (id [_]
    id)

  pcom/IDestroy
  (destroy! [_]
    (artifact-player#destroy! state))

  proto/ISelectRegion
  (set-region! [this opts]
    (artifact-player#set-region! this state opts))
  (region [_]
    (:region @state))

  proto/ILoad
  (load! [this opts]
    (artifact-player#load! this id state *artifact opts))
  (ready? [_]
    (:ready? @state))
  (error? [_]
    (:error? @state))

  proto/IPlayer
  (play-pause! [this]
    (artifact-player#play-pause! this state))
  (playing? [this]
    (artifact-player#playing? this state)))
