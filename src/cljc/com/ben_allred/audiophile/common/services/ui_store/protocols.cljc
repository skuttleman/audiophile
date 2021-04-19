(ns com.ben-allred.audiophile.common.services.ui-store.protocols)

(defprotocol IStore
  (get-state [this] "returns the current state of the store")
  (dispatch! [this action] "dispatches an action to (potentially) update the store"))
