(ns com.ben-allred.audiophile.ui.services.forms.standard
  (:require
    [com.ben-allred.audiophile.common.services.forms.protocols :as pforms]
    [com.ben-allred.audiophile.common.services.stubs.reagent :as r]
    [com.ben-allred.audiophile.common.utils.maps :as maps]))

(deftype StandardForm [state errors-fn]
  pforms/IChange
  (init! [_ value]
    (let [rep (maps/flatten value)]
      (swap! state assoc
             :init rep
             :current rep
             :visited #{}
             :form/visited false)
      nil))
  (update! [_ path value]
    (swap! state (fn [val]
                   (-> val
                       (update :current (if (some? value) assoc dissoc) path value)
                       (update :visited conj path))))
    nil)

  pforms/ITrack
  (visit! [_]
    (swap! state assoc :form/visited true)
    nil)
  (visit! [_ path]
    (swap! state update :visited conj path)
    nil)
  (visited? [_]
    (let [val @state]
      (or (:form/visited val)
          (boolean (seq (:visited val))))))
  (visited? [_ path]
    (let [val @state]
      (or (:form/visited val)
          (contains? (:visited val) path))))

  pforms/IValidate
  (errors [this]
    (errors-fn @this))

  IDeref
  (-deref [_]
    (maps/nest (:current @state))))

(defn create
  ([]
   (create nil))
  ([init-value]
   (create init-value (constantly nil)))
  ([init-value errors-fn]
   (let [state (r/atom nil)]
     (doto (->StandardForm state errors-fn)
       (pforms/init! init-value)))))
