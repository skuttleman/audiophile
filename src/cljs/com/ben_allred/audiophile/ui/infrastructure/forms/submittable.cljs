(ns com.ben-allred.audiophile.ui.infrastructure.forms.submittable
  (:require
    [com.ben-allred.audiophile.common.core.resources.core :as res]
    [com.ben-allred.audiophile.ui.infrastructure.forms.core :as forms]
    [com.ben-allred.audiophile.ui.infrastructure.forms.protocols :as pforms]
    [com.ben-allred.vow.core :as v]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]))

(deftype SubmittableForm [*resource *form local->remote remote->local]
  pforms/IInit
  (init! [_ value]
    (forms/init! *form value))

  pforms/IAttempt
  (attempt! [this]
    (forms/attempt! *form)
    (-> @this
        local->remote
        (->> (res/request! *resource))
        (v/then remote->local)
        (v/peek (partial forms/init! this) nil)))
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
