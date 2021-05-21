(ns com.ben-allred.audiophile.common.services.resources.cached
  (:require
    [com.ben-allred.audiophile.common.services.resources.core :as res]
    [com.ben-allred.audiophile.common.services.resources.protocols :as pres]
    [com.ben-allred.audiophile.common.services.stubs.reagent :as r]
    [com.ben-allred.vow.core :as v]
    [com.ben-allred.vow.impl.protocol :as pv]
    [integrant.core :as ig])
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

(defmethod ig/init-key ::resource [_ {:keys [resource]}]
  (->CachedResource (r/atom nil) resource))
