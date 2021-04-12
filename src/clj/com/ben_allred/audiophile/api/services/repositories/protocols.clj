(ns com.ben-allred.audiophile.api.services.repositories.protocols)

(defprotocol ITransact
  (transact! [this f]))

(defprotocol IExecute
  (execute! [this query opts])
  (exec-raw! [this sql opts]))
