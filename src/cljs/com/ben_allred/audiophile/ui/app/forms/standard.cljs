(ns com.ben-allred.audiophile.ui.app.forms.standard
  (:require
    [com.ben-allred.audiophile.ui.core.forms.protocols :as pforms]
    [com.ben-allred.audiophile.ui.core.utils.reagent :as r]
    [com.ben-allred.audiophile.common.core.utils.maps :as maps]))

(deftype StandardForm [state errors-fn]
  pforms/IInit
  (init! [_ value]
    (let [rep (maps/flatten value)]
      (swap! state assoc
             :init rep
             :current rep
             :form/attempted false
             :touched #{}
             :form/touched false)
      nil))

  pforms/IAttempt
  (attempt! [_]
    (swap! state assoc :form/attempted true)
    nil)
  (attempted? [_]
    (:form/attempted @state))

  pforms/IChange
  (change! [_ path value]
    (swap! state (fn [val]
                   (update val
                           :current
                           (if (some? value)
                             assoc
                             dissoc)
                           path
                           value)))
    nil)
  (changed? [_]
    (let [{:keys [current init]} @state]
      (= current init)))
  (changed? [_ path]
    (let [{:keys [current init]} @state]
      (= (get current path) (get init path))))

  pforms/ITrack
  (touch! [_]
    (swap! state assoc :form/touched true)
    nil)
  (touch! [_ path]
    (swap! state update :touched conj path)
    nil)
  (touched? [_]
    (let [val @state]
      (or (:form/touched val)
          (boolean (seq (:touched val))))))
  (touched? [_ path]
    (let [val @state]
      (or (:form/touched val)
          (contains? (:touched val) path))))

  pforms/IValidate
  (errors [this]
    (errors-fn @this))

  IDeref
  (-deref [_]
    (maps/nest (:current @state))))

(defn create
  "Creates a validated local form with the initial value set to `init-value` which
   should be a map or `nil`. `errors-fn` should take the (potentially nested) model and
   return a similarly shaped data structure where all the leaf keys either have the
   value nil (or are present) and all the leaf values are either `nil` or a non-empty
   sequence of strings (error messages). Leaf nodes of your data model should be
   updated atomically.

   ```clojure
   (errors-fn {:my {:nested {:data :model} :is #{:awesome!}}})
   ;; => nil
   ;; => {:my {:nested {:data [\"should be foo\" \"should be baz\"]}}}
   ;; => {:my {:nested {:is [\"not so awesome\"]}}
   ```"
  ([]
   (create nil))
  ([init-value]
   (create init-value (constantly nil)))
  ([init-value errors-fn]
   {:pre [(or (nil? init-value) (map? init-value))]}
   (let [state (r/atom nil)]
     (doto (->StandardForm state errors-fn)
       (pforms/init! init-value)))))
