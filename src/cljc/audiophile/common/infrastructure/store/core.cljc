(ns audiophile.common.infrastructure.store.core
  (:require
    [audiophile.common.core.utils.logger :as log]
    [audiophile.common.infrastructure.store.protocols :as pstore]
    [com.ben-allred.vow.core :as v]))

(defmulti async* (fn [[type] _]
                   type))

(defmethod async* :default
  [action {:keys [store]}]
  (pstore/reduce! store action))

(defmulti mutate* (fn [_ [type]]
                      type))

(defmethod mutate* :default
  [state _]
  state)

(defn init! [store system]
  (pstore/init! store system))

(defn dispatch! [store [type :as action]]
  {:pre [(keyword? type) (namespace type)]}
  (-> (pstore/with-system store async* action)
      v/resolve
      (v/then (constantly nil))))
