(ns com.ben-allred.audiophile.common.app.resources.cached
  (:require
    [com.ben-allred.audiophile.common.core.resources.core :as res]
    [com.ben-allred.audiophile.common.core.resources.protocols :as pres]
    [com.ben-allred.audiophile.common.core.stubs.reagent :as r]
    [com.ben-allred.vow.core :as v]
    [com.ben-allred.vow.impl.protocol :as pv])
  #?(:clj
     (:import
       (clojure.lang IDeref))))

(deftype CachedResource [state *resource]
  pres/IResource
  (request! [this opts]
    (locking this
      (or @state
          (reset! state (res/request! *resource opts)))))
  (status [_]
    (res/status *resource))

  pv/IPromise
  (then [_ on-success on-error]
    (v/then *resource on-success on-error))

  IDeref
  (#?(:cljs -deref :default deref) [_]
    @*resource))

(defn resource [{:keys [resource]}]
  (->CachedResource (r/atom nil) resource))
