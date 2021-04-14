(ns com.ben-allred.audiophile.common.services.ui-store.protocols)

(defprotocol IStore
  (get-state [this])
  (dispatch! [this action]))

(defprotocol IResource
  (request! [this opts]))
