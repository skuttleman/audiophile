(ns audiophile.ui.forms.standard
  (:require
    [audiophile.common.core.utils.maps :as maps]
    [audiophile.common.core.utils.uuids :as uuids]
    [audiophile.common.infrastructure.protocols :as pcom]
    [audiophile.common.infrastructure.store.core :as store]
    [audiophile.ui.forms.protocols :as pforms]
    [audiophile.ui.store.actions :as act]))

(deftype StandardForm [id store errors-fn]
  pcom/IIdentify
  (id [_]
    id)

  pforms/IInit
  (init! [_ value]
    (let [rep (maps/flatten value)]
      (store/dispatch! store (act/form:init id {:init           rep
                                                :current        rep
                                                :form/attempted false
                                                :touched        #{}
                                                :form/touched   false}))))

  pcom/IDestroy
  (destroy! [_]
    (store/dispatch! store (act/form:cleanup id)))

  pforms/IAttempt
  (attempt! [_]
    (store/dispatch! store (act/form:update id merge {:form/attempted true})))
  (attempted? [_]
    (get-in @store [:forms id :form/attempted]))
  (attempting? [_]
    false)

  pforms/IChange
  (change! [_ path value]
    (let [[f args] (if (some? value)
                     [assoc-in [[:current path] value]]
                     [update [:current dissoc path]])]
      (store/dispatch! store (apply act/form:update id f args))))
  (changed? [_]
    (let [{:keys [current init]} (get-in @store [:forms id])]
      (= current init)))
  (changed? [_ path]
    (let [{:keys [current init]} (get-in @store [:forms id])]
      (= (get current path) (get init path))))

  pforms/ITrack
  (touch! [_]
    (store/dispatch! store (act/form:update id merge {:form/touched true})))
  (touch! [_ path]
    (store/dispatch! store (act/form:update id update :touched conj path)))
  (touched? [_]
    (let [val (get-in @store [:forms id])]
      (or (:form/touched val)
          (boolean (seq (:touched val))))))
  (touched? [_ path]
    (let [val (get-in @store [:forms id])]
      (or (:form/touched val)
          (contains? (:touched val) path))))

  pforms/IValidate
  (errors [this]
    (errors-fn @this))

  IDeref
  (-deref [_]
    (maps/nest (get-in @store [:forms id :current]))))

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
  ([store]
   (create store nil))
  ([store init-value]
   (create store init-value (constantly nil)))
  ([store init-value errors-fn]
   (create (uuids/random) store init-value errors-fn))
  ([id store init-value errors-fn]
   {:pre [(or (nil? init-value) (map? init-value))]}
   (doto (->StandardForm id store errors-fn)
     (pforms/init! init-value))))
