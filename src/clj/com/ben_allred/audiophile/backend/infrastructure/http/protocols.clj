(ns com.ben-allred.audiophile.backend.infrastructure.http.protocols)

(defprotocol ICheckHealth
  "Protocol mixin to supply heath check details"
  (display-name [this] "The name used to identify the component")
  (healthy? [this] "Is the component healthy")
  (details [this] "Details related to the component's health check"))
