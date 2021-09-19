(ns com.ben-allred.audiophile.ui.api.forms.submittable
  (:require
    [com.ben-allred.audiophile.common.core.resources.core :as res]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.ui.core.forms.core :as forms]
    [com.ben-allred.audiophile.ui.core.forms.protocols :as pforms]
    [com.ben-allred.vow.core :as v]
    [com.ben-allred.vow.impl.protocol :as pv]))

(defn ^:private converter [id _]
  id)

(defmulti remote->internal converter)

(defmulti internal->remote converter)

(defn ^:private attempt* [id *form *resource opts]
  (if-let [errors (forms/errors *form)]
    (do (forms/touch! *form)
        (v/reject [:local/rejected errors]))
    (-> *resource
        (res/request! opts)
        (v/then (partial remote->internal id))
        (v/peek (fn [result]
                  (let [result (:form/reset-to opts result)]
                    (forms/init! *form result)))
                nil))))

(defmethod remote->internal :default
  [_ data]
  data)

(defmethod internal->remote :default
  [_ data]
  data)

(deftype ValidatedResource [id *resource *form opts*]
  pforms/IInit
  (init! [_ value]
    (forms/init! *form value))

  pforms/IAttempt
  (attempt! [this]
    (forms/attempt! *form)
    (attempt* id this *resource (assoc opts*
                                       :form/value
                                       (internal->remote id @*form))))
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
   (create resource form nil))
  ([resource form opts]
   (create :default resource form opts))
  ([id resource form opts]
   (->ValidatedResource id resource form opts)))

(defn opts->request [_]
  (fn [opts]
    {:body        {:data (:form/value opts)}
     :http/async? true}))
