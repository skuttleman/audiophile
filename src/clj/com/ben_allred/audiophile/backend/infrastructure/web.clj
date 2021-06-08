(ns com.ben-allred.audiophile.backend.infrastructure.web
  (:require
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [immutant.web :as web*]))

(defn server [{:keys [handler server-port]}]
  (let [server (web*/run handler {:port server-port :host "0.0.0.0"})]
    (log/info (str "[SERVER] is listening on port " server-port))
    server))

(defn server#stop [server]
  (log/info "[SERVER] is shutting down")
  (web*/stop server))
