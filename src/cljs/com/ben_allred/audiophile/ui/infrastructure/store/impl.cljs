(ns com.ben-allred.audiophile.ui.infrastructure.store.impl
  (:require
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.common.api.store.protocols :as pstore]
    [com.ben-allred.audiophile.ui.infrastructure.store.reducers :as reducers]
    [com.ben-allred.collaj.core :as collaj]
    [com.ben-allred.collaj.enhancers :as ecollaj]
    [reagent.core :as r]))

(deftype Store [get-state dispatch]
  pstore/IStore
  (dispatch! [_ action]
    (dispatch action))

  IDeref
  (-deref [_]
    (get-state)))

(def ^{:arglists '([reducer])} create*
  (memoize (fn [reducer]
             (let [store (collaj/create-custom-store
                           r/atom
                           reducer
                           (reducer)
                           (ecollaj/with-log-middleware
                             #(log/info "Action dispatched:" %)
                             #(log/info "New state:" %)))]
               (->Store (:get-state store) (:dispatch store))))))

(defn create [_]
  (create* reducers/reducer))
