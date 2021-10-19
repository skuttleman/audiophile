(ns com.ben-allred.audiophile.ui.infrastructure.resources.cached
  (:require
    [com.ben-allred.audiophile.common.core.resources.protocols :as pres]
    [com.ben-allred.audiophile.ui.core.utils.reagent :as r]))

(deftype CachedResource [state *resource]
  pres/IResource
  (request! [_ opts]
    (or @state
        (reset! state (pres/request! *resource opts))))
  (status [_]
    (pres/status *resource))

  IDeref
  (-deref [_]
    @*resource))

(defn resource [{:keys [resource]}]
  (->CachedResource (r/atom nil) resource))
