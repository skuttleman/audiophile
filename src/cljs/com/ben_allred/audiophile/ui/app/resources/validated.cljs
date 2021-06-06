(ns com.ben-allred.audiophile.ui.app.resources.validated
  (:require
    [com.ben-allred.audiophile.ui.core.forms.core :as forms]
    [com.ben-allred.audiophile.ui.core.forms.protocols :as pforms]
    [com.ben-allred.audiophile.common.core.resources.protocols :as pres]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.vow.core :as v]
    [com.ben-allred.vow.impl.protocol :as pv]))

(defn ^:private converter [id _]
  id)

(defmulti remote->internal converter)
(defmethod remote->internal :default
  [_ data]
  data)

(defmulti internal->remote converter)
(defmethod internal->remote :default
  [_ data]
  data)

(deftype ValidatedResource [id *resource *form opts*]
  pres/IResource
  (request! [this opts]
    (let [opts (-> opts*
                   (merge opts)
                   (assoc :form/value (internal->remote id @*form)))]
      (forms/attempt! this)
      (if-let [errors (forms/errors this)]
        (do (forms/touch! *form)
            (v/reject [:local/rejected errors]))
        (-> *resource
            (pres/request! opts)
            (v/then (partial remote->internal id))
            (v/peek (partial forms/init! *form) nil)))))
  (status [_]
    (pres/status *resource))

  pforms/IInit
  (init! [_ value]
    (forms/init! *form value))

  pforms/IAttempt
  (attempt! [_]
    (forms/attempt! *form))
  (attempted? [_]
    (forms/attempted? *form))

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

  pv/IPromise
  (then [_ on-success on-error]
    (-> *resource
        (v/then (partial remote->internal id))
        (v/then on-success on-error)))

  IDeref
  (-deref [_]
    @*form))

(defn create
  ([resource form]
   (create :default resource form nil))
  ([resource form opts]
   (create :default resource form opts))
  ([id resource form opts]
   (->ValidatedResource id resource form opts)))

(defn opts->request [_]
  (fn [opts]
    {:body {:data (:form/value opts)}}))
