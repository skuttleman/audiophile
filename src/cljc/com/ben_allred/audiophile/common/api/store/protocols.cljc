(ns com.ben-allred.audiophile.common.api.store.protocols)

(defprotocol IStore
  "An interface for managing a reducing store"
  (dispatch! [this action] "Dispatches an action to (potentially) update the store"))
