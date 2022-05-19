(ns com.ben-allred.audiophile.common.api.store.core
  (:require
    [com.ben-allred.audiophile.common.api.store.protocols :as pstore]))

(defn dispatch! [store action]
  (if (fn? action)
    (action store)
    (pstore/dispatch! store action)))
