(ns com.ben-allred.audiophile.common.app.forms.noop
  (:require
    [com.ben-allred.audiophile.common.core.forms.protocols :as pforms])
  #?(:clj
     (:import
       (clojure.lang IDeref))))

(deftype NoopForm [value]
  pforms/IInit
  (init! [_ _]
    nil)

  pforms/IChange
  (change! [_ _ _]
    nil)

  pforms/ITrack
  (touch! [_]
    nil)
  (touch! [_ _]
    nil)
  (touched? [_]
    false)
  (touched? [_ _]
    false)

  pforms/IValidate
  (errors [_]
    nil)

  IDeref
  (#?(:cljs -deref :default deref) [_]
    value))

(defn create
  "Creates a form that stubs all operations and always produces the same value when `deref`'ed.
   Supports 2-arity to match other form constructors."
  ([]
   (->NoopForm nil))
  ([value]
   (->NoopForm value))
  ([value _]
   (->NoopForm value)))
