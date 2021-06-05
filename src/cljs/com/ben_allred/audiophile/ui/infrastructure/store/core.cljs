(ns com.ben-allred.audiophile.ui.infrastructure.store.core
  (:require
    [com.ben-allred.audiophile.ui.core.utils.reagent :as r]
    [com.ben-allred.audiophile.ui.infrastructure.store.protocols :as pstore]
    [com.ben-allred.audiophile.ui.infrastructure.store.reducers :as reducers]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.collaj.core :as collaj]
    [com.ben-allred.collaj.enhancers :as ecollaj]))

(deftype Store [get-state dispatch]
  pstore/IStore
  (get-state [_]
    (get-state))
  (dispatch! [_ action]
    (dispatch action)))

(defn ^:private create* [reducer init]
  (let [{:keys [get-state dispatch]}
        (collaj/create-custom-store
          r/atom
          reducer
          (merge (reducer) init)
          (ecollaj/with-log-middleware
            #(log/info "Action dispatched:" %)
            #(log/info "New state:" %)))]
    (->Store get-state dispatch)))

(defn store [{:keys [env]}]
  (create* reducers/reducer env))

(defn get-state [store]
  (pstore/get-state store))

(defn dispatch! [store action]
  (if (fn? action)
    (action store)
    (pstore/dispatch! store action)))
