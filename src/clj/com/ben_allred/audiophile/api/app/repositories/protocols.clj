(ns com.ben-allred.audiophile.api.app.repositories.protocols
  (:refer-clojure :exclude [format get]))

(defprotocol ITransact
  "Opens a transaction and invokes `f` with a query [[IExecute]] which can be used to
   do multiple operations inside the transaction. The transaction is committed when
   `f` returns a value. The transaction is aborted when `f` throws any exception.
   Returns or throws the result of `f`."
  (transact! [this f]))

(defprotocol IExecute
  "Executes a single query or request and returns the results. Typically run inside a transaction."
  (execute! [this query opts]))

(defprotocol IFormatQuery
  "Formats a query to be executed."
  (format [this query]))

(defprotocol IKVStore
  "A key/value store"
  (uri [this key opts] "Generates a uri for accessing the resource")
  (get [this key opts] "Get value from store at key")
  (put! [this key content opts] "Put value in store at key"))
