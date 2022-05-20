(ns com.ben-allred.audiophile.common.infrastructure.store.core
  (:require
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.common.infrastructure.store.protocols :as pstore]
    [com.ben-allred.vow.core :as v]))

(defmulti async* (fn [[type] _]
                   type))

(defmethod async* :default
  [action {:keys [store]}]
  (pstore/reduce! store action))

(defn init! [store system]
  (pstore/init! store system))

(defn dispatch! [store [type :as action]]
  {:pre [(keyword? type) (namespace type)]}
  (v/resolve (pstore/with-system store async* action)))
