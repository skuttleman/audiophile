(ns com.ben-allred.audiophile.common.services.resources.validated
  (:require
    [com.ben-allred.audiophile.common.services.forms.core :as forms]
    [com.ben-allred.audiophile.common.services.forms.protocols :as pforms]
    [com.ben-allred.audiophile.common.services.resources.protocols :as pres]
    [com.ben-allred.vow.core :as v]
    [com.ben-allred.vow.impl.protocol :as pv])
  #?(:clj
     (:import
       (clojure.lang IDeref))))

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

(defn ^:private request* [id resource form opts success-k error-k]
  (-> resource
      (pres/request! opts)
      (v/then (partial remote->internal id))
      (v/then (fn [value]
                (forms/init! form value)
                [success-k value])
              (fn [errors]
                (v/reject [error-k errors])))))

(deftype ValidatedResource [id resource form]
  pres/IResource
  (request! [this opts]
    (let [opts (assoc opts :form/value (internal->remote id @form))]
      (case (:target opts)
        :init (request* id
                        resource
                        form
                        opts
                        :remote/initialized
                        :remote/init.error)
        (if-let [errors (forms/errors this)]
          (do (forms/visit! form)
              (v/reject [:local/rejected errors]))
          (request* id
                    resource
                    form
                    opts
                    :remote/accepted
                    :remote/rejected)))))
  (status [_]
    (pres/status resource))

  pforms/IChange
  (init! [_ value]
    (forms/init! form value))
  (update! [_ path value]
    (forms/update! form path value))

  pforms/ITrack
  (visit! [_]
    (forms/visit! form))
  (visit! [_ path]
    (forms/visit! form path))
  (visited? [_]
    (forms/visited? form))
  (visited? [_ path]
    (forms/visited? form path))
  (changed? [_ path]
    (forms/changed? form path))

  pforms/IValidate
  (errors [_]
    (forms/errors form))

  pv/IPromise
  (then [_ on-success on-error]
    (-> resource
        (v/then (partial remote->internal id))
        (v/then on-success on-error)))

  IDeref
  (#?(:cljs -deref :default deref) [_]
    @form))

(defn create
  ([resource form]
   (create :default resource form))
  ([id resource form]
   (->ValidatedResource id resource form)))
