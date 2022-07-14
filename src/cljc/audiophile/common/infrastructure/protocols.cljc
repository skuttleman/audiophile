(ns audiophile.common.infrastructure.protocols)

(defprotocol IIdentify
  "Abstraction for a self identifying entity."
  (id [this] "Returns the id"))

(defprotocol IInit
  "A component that can initialize its internal resources"
  (init! [this]
    "Creates any required internal resources.
     Call this before using the object."))

(defprotocol IDestroy
  "A component that can destroy its internal resources"
  (destroy! [this]
    "Destroys any internal resources.
     Call this when the object is no longer needed."))
