(ns audiophile.backend.infrastructure.web
  (:require
    [audiophile.common.core.utils.logger :as log]
    [immutant.web :as web*]))

(defn server
  "Creates and starts a web server.

  ```clojure
  (server {:handler (constantly {:status 204}) :server-port 1234})
  ```"
  [{:keys [handler server-port]}]
  (let [server (web*/run handler {:port server-port :host "0.0.0.0"})]
    (log/with-ctx :SERVER
      (log/info "listening on port" server-port))
    server))

(defn server#stop
  "Stops a running web server.

  ```clojure
  (let [web-server (server {:handler (constantly {:status 204}) :server-port 1234})]
    ...
    (server#stop server))
  ```"
  [server]
  (log/with-ctx :SERVER
    (log/info "shutting down"))
  (web*/stop server))
