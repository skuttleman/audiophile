(ns audiophile.common.infrastructure.http.protocols)

(defprotocol ICheckHealth
  "Protocol mixin to supply health check details"
  (display-name [this] "The name used to identify the component")
  (healthy? [this] "Is the component healthy")
  (details [this] "Details related to the component's health check. Should be a map or nil."))
