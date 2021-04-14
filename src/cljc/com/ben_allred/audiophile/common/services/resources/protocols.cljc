(ns com.ben-allred.audiophile.common.services.resources.protocols)

(defprotocol IResource
  (request! [this opts]))
