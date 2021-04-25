(ns com.ben-allred.audiophile.api.services.repositories.protocols
  (:refer-clojure :exclude [format]))

(defprotocol ITransact
  (transact! [this f]))

(defprotocol IExecute
  (exec-raw! [this sql opts])
  (execute! [this query opts]))

(defprotocol IFormatQuery
  (format [this query]))
