(ns audiophile.ui.forms.submittable
  (:require
    [audiophile.common.core.utils.logger :as log]
    [audiophile.common.infrastructure.protocols :as pcom]
    [audiophile.common.infrastructure.resources.core :as res]
    [audiophile.ui.forms.core :as forms]
    [audiophile.ui.forms.protocols :as pforms]
    [com.ben-allred.vow.core :as v]))

(deftype SubmittableForm [id *resource *form local->remote remote->local]
  pcom/IIdentify
  (id [_]
    id)

  pforms/IInit
  (init! [_ value]
    (forms/init! *form value))

  pcom/IDestroy
  (destroy! [_]
    (pcom/destroy! *resource)
    (pcom/destroy! *form))

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
  ([id *form *resource]
   (create id *form *resource identity identity))
  ([id *form *resource local->remote remote->local]
   (->SubmittableForm id *resource *form local->remote remote->local)))
