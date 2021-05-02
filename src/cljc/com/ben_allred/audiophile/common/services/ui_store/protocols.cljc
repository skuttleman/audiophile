(ns com.ben-allred.audiophile.common.services.ui-store.protocols)

(defprotocol IStore
  "An interface for managing a reducing store"
  (get-state [this] "Returns the current state of the store")
  (dispatch! [this action] "Dispatches an action to (potentially) update the store"))
