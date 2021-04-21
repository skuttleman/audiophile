(ns com.ben-allred.audiophile.api.services.repositories.protocols)

(defprotocol ITransact
  (transact! [this f]))

(defprotocol IExecute
  (exec-raw! [this sql opts])
  (execute! [this query opts]))
