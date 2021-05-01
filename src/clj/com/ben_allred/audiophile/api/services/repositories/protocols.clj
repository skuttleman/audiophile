(ns com.ben-allred.audiophile.api.services.repositories.protocols
  (:refer-clojure :exclude [format]))

(defprotocol ITransact
  "Opens a database transaction and invokes `f` with an `IExecute`
   implementation and a map of additional transaction details. Returns the result of `f`."
  (transact! [this f]))

(defprotocol IExecute
  "Sends queries across a database connection and returns the results"
  (exec-raw! [this sql opts])
  (execute! [this query opts]))

(defprotocol IFormatQuery
  "Formats a query into raw SQL."
  (format [this query]))
