(ns audiophile.ui.forms.submittable
  (:require
    [audiophile.common.core.utils.logger :as log]
    [audiophile.common.infrastructure.resources.core :as res]
    [audiophile.ui.forms.core :as forms]
    [audiophile.ui.forms.protocols :as pforms]
    [com.ben-allred.vow.core :as v]))

(deftype SubmittableForm [*resource *form local->remote remote->local]
  pforms/ILifeCycle
  (init! [_ value]
    (forms/init! *form value))
  (destroy! [_]
    (pforms/destroy! *form))

  pforms/IAttempt
  (attempt! [this]
    (forms/attempt! *form)
    (if-let [errors (forms/errors this)]
      (v/reject errors)
      (-> @this
          local->remote
          (->> (res/request! *resource))
          (v/then remote->local)
          (v/peek (partial forms/init! this) nil))))
  (attempted? [_]
    (forms/attempted? *form))
  (attempting? [_]
    (res/requesting? *resource))

  pforms/IChange
  (change! [_ path value]
    (forms/change! *form path value))
  (changed? [_]
    (forms/changed? *form))
  (changed? [_ path]
    (forms/changed? *form path))

  pforms/ITrack
  (touch! [_]
    (forms/touch! *form))
  (touch! [_ path]
    (forms/touch! *form path))
  (touched? [_]
    (forms/touched? *form))
  (touched? [_ path]
    (forms/touched? *form path))

  pforms/IValidate
  (errors [_]
    (forms/errors *form))

  IDeref
  (-deref [_]
    @*form))

(defn create
  ([*form *resource]
   (create *form *resource identity identity))
  ([*form *resource local->remote remote->local]
   (->SubmittableForm *resource *form local->remote remote->local)))
