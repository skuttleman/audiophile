(ns com.ben-allred.audiophile.api.services.repositories.protocols
  (:refer-clojure :exclude [format get]))

(defprotocol ITransact
  "Opens a database transaction and invokes `f` with an [[IExecute]]
   implementation and a map of additional transaction options. Returns the result of `f`."
  (transact! [this f]))

(defprotocol IExecute
  "Sends queries across a database connection and returns the results. Typically run inside a transaction."
  (execute! [this query opts] "Execute a single query"))

(defprotocol IFormatQuery
  "Formats a query to be executedL."
  (format [this query]))

(defprotocol IKVStore
  "A key/value store"
  (uri [this key opts] "Generates a uri for accessing the resource")
  (get [this key opts] "Get value from store at key")
  (put! [this key content opts] "Put value in store at key"))
