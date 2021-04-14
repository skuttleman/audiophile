(ns com.ben-allred.audiophile.common.services.ui-store.core
  (:require
    [com.ben-allred.audiophile.common.services.ui-store.protocols :as pui-store]
    [com.ben-allred.audiophile.common.services.ui-store.reducers :as reducers]
    [com.ben-allred.audiophile.common.utils.logger :as log]
    [com.ben-allred.audiophile.common.utils.stubs :as st]
    [com.ben-allred.collaj.core :as collaj]
    [com.ben-allred.collaj.enhancers :as ecollaj]
    [integrant.core :as ig]))

(deftype Store [get-state dispatch]
  pui-store/IStore
  (get-state [_]
    (get-state))
  (dispatch! [_ action]
    (dispatch action)))

(defn create-store [reducer]
  (let [{:keys [get-state dispatch]}
        (collaj/create-custom-store
          st/atom
          reducer
          ecollaj/with-fn-dispatch
          #?(:cljs (ecollaj/with-log-middleware
                     #(log/info "Action dispatched:" %)
                     #(log/info "New state:" %))))]
    (->Store get-state dispatch)))

(defmethod ig/init-key ::store [_ _]
  (create-store reducers/reducer))

(defn get-state [store]
  (pui-store/get-state store))

(defn dispatch! [store action]
  (pui-store/dispatch! store action))
