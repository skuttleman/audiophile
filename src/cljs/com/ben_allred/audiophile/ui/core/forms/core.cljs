(ns com.ben-allred.audiophile.ui.core.forms.core
  (:require
    [com.ben-allred.audiophile.ui.core.forms.protocols :as pforms]
    [com.ben-allred.audiophile.common.core.resources.core :as res]
    [com.ben-allred.audiophile.common.core.resources.protocols :as pres]
    [com.ben-allred.audiophile.common.core.utils.fns :as fns]
    [com.ben-allred.audiophile.common.core.utils.maps :as maps]))

(defn ^:private derefable? [x]
  (satisfies? IDeref x))

(defn init!
  ([*form]
   (pforms/init! *form))
  ([*form value]
   (pforms/init! *form value)))

(defn change! [*form path value]
  (pforms/change! *form path value))

(defn touch!
  ([*form]
   (pforms/touch! *form))
  ([*form path]
   (pforms/touch! *form path)))

(defn touched?
  ([*form]
   (pforms/touched? *form))
  ([*form path]
   (pforms/touched? *form path)))

(defn errors [*form]
  (pforms/errors *form))

(defn valid? [*form]
  (empty? (errors *form)))

(defn update-qp! [*link f & f-args]
  (pforms/update-qp! *link #(apply f % f-args))
  nil)

(defn with-attrs
  ([*form path]
   (with-attrs nil *form path))
  ([attrs *form path]
   (let [tracks? (satisfies? pforms/ITrack *form)
         visited? (when tracks?
                    (touched? *form path))
         errors (when (satisfies? pforms/IValidate *form)
                  (get-in (errors *form) path))]
     (-> attrs
         (assoc :visited? visited?)
         (maps/assoc-defaults :disabled (when (satisfies? pres/IResource *form)
                                          (res/requesting? *form))
                              :on-blur (constantly nil))

         (cond->
           tracks?
           (update :on-blur fns/sidecar! (fn [_]
                                           (touch! *form path)))

           (derefable? *form)
           (assoc :value (get-in @*form path))

           (satisfies? pforms/IChange *form)
           (assoc :on-change (partial pforms/change! *form path))

           (and visited? errors)
           (assoc :errors errors))))))
