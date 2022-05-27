(ns audiophile.common.infrastructure.protocols)

(defprotocol IIdentify
  "Abstraction for a self identifying entity."
  (id [this] "Returns the id"))

(defprotocol IDestroy
  "Can initialize or re-initialize itself to an internally defined immutable value."
  (destroy! [this]
    "Destroys any internal resources.
     Call this when the object is no longer needed."))
