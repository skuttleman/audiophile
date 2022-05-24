(ns audiophile.common.infrastructure.store.protocols)

(defprotocol IStore
  "An interface for managing a reducing store"
  (reduce! [this action] "Dispatches an action to (potentially) update the store"))

(defprotocol IAsyncStore
  (init! [this system])
  (with-system [this f action]))
