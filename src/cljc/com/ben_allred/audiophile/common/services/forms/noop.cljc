(ns com.ben-allred.audiophile.common.services.forms.noop
  (:require
    [com.ben-allred.audiophile.common.services.forms.protocols :as pforms])
  #?(:clj
     (:import
       (clojure.lang IDeref))))

(deftype NoopForm [value]
  pforms/IChange
  (init! [_ _]
    nil)
  (update! [_ _ _]
    nil)

  pforms/ITrack
  (visit! [_]
    nil)
  (visit! [_ _]
    nil)
  (visited? [_]
    false)
  (visited? [_ _]
    false)

  pforms/IValidate
  (errors [_]
    nil)

  IDeref
  (#?(:cljs -deref :default deref) [_]
    value))

(defn create
  ([]
   (->NoopForm nil))
  ([value]
   (->NoopForm value))
  ([value _]
   (->NoopForm value)))
