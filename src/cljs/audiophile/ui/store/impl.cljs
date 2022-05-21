(ns audiophile.ui.store.impl
  (:require
    [audiophile.common.core.utils.logger :as log]
    [audiophile.common.infrastructure.store.core :as store]
    [audiophile.common.infrastructure.store.protocols :as pstore]
    [com.ben-allred.collaj.core :as collaj]
    [com.ben-allred.collaj.enhancers :as ecollaj]
    [reagent.core :as r]))

(deftype Store [get-state dispatch system]
  pstore/IStore
  (reduce! [_ action]
    (dispatch action)
    nil)

  pstore/IAsyncStore
  (init! [_ sys]
    (vreset! system sys))
  (with-system [this f action]
    (f action (or @system {:store this})))

  IDeref
  (-deref [_]
    (get-state)))

(def ^{:arglists '([reducer])} create*
  (memoize (fn [reducer]
             (let [store (collaj/create-custom-store
                           r/atom
                           reducer
                           {}
                           (ecollaj/with-log-middleware
                             #(log/info "Action dispatched:" %)
                             #(log/info "New state:" %)))]
               (->Store (:get-state store) (:dispatch store) (volatile! nil))))))

(defn create [_]
  (create* store/mutate*))
