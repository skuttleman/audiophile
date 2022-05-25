(ns audiophile.ui.forms.core
  (:require
    [audiophile.common.core.utils.fns :as fns]
    [audiophile.common.core.utils.logger :as log]
    [audiophile.common.core.utils.maps :as maps]
    [audiophile.ui.forms.protocols :as pforms]
    [clojure.string :as string]))

(defn ^:private derefable? [x]
  (satisfies? IDeref x))

(defn init!
  ([*form]
   (pforms/init! *form)
   nil)
  ([*form value]
   (pforms/init! *form value)
   nil))

(defn attempt! [*form]
  (pforms/attempt! *form))

(defn attempted? [*form]
  (pforms/attempted? *form))

(defn attempting? [*form]
  (pforms/attempting? *form))

(defn change! [*form path value]
  (pforms/change! *form path value)
  nil)

(defn changed?
  ([*form]
   (pforms/changed? *form))
  ([*form path]
   (pforms/changed? *form path)))

(defn touch!
  ([*form]
   (pforms/touch! *form)
   nil)
  ([*form path]
   (pforms/touch! *form path)
   nil))

(defn touched?
  ([*form]
   (pforms/touched? *form))
  ([*form path]
   (pforms/touched? *form path)))

(defn errors [*form]
  (pforms/errors *form))

(defn valid? [*form]
  (empty? (errors *form)))

(defn with-attrs
  ([*form path]
   (with-attrs nil *form path))
  ([attrs *form path]
   (let [tracks? (satisfies? pforms/ITrack *form)
         visited? (when tracks?
                    (touched? *form path))
         errors (when (satisfies? pforms/IValidate *form)
                  (get-in (errors *form) path))
         [attempted? attempting?] (when (satisfies? pforms/IAttempt *form)
                                    [(attempted? *form)
                                     (attempting? *form)])
         class (string/join "-" (map name path))]
     (-> attrs
         (assoc :visited? visited? :attempted? attempted? :errors errors)
         (update :class (fnil conj []) class)
         (maps/assoc-defaults :disabled attempting?
                              :on-blur (constantly nil))

         (cond->
           tracks?
           (update :on-blur fns/sidecar! (fn [_]
                                           (touch! *form path)))

           (derefable? *form)
           (assoc :value (get-in @*form path))

           (satisfies? pforms/IChange *form)
           (assoc :on-change (partial pforms/change! *form path)))))))
