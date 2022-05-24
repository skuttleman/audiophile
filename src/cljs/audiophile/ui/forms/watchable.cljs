(ns audiophile.ui.forms.watchable
  (:require
    [audiophile.common.core.utils.maps :as maps]
    [audiophile.ui.forms.protocols :as pforms]
    [reagent.core :as r]))

(deftype WatchableForm [state]
  pforms/IChange
  (change! [_ path value]
    (let [f (if (some? value) #(assoc % path value) #(dissoc % path))]
      (swap! state update :current f)))
  (changed? [_]
    (let [{:keys [current init]} @state]
      (= current init)))
  (changed? [_ path]
    (let [{:keys [current init]} @state]
      (= (get current path) (get init path))))

  IDeref
  (-deref [_]
    (maps/nest (:current @state)))

  IWatchable
  (-notify-watches [_ old-val new-val]
    (-notify-watches state old-val new-val))
  (-add-watch [this key f]
    (-add-watch state key (fn [k _ old new]
                            (f k
                               this
                               (maps/nest (:current old))
                               (maps/nest (:current new))))))
  (-remove-watch [_ key]
    (-remove-watch state key)))

(defn create
  ([]
   (create nil))
  ([init-value]
   {:pre [(or (nil? init-value) (map? init-value))]}
   (->WatchableForm (r/atom {:current (maps/flatten init-value)}))))
