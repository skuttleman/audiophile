(ns com.ben-allred.audiophile.api.core
  (:require
    [com.ben-allred.audiophile.common.utils.logger :as log]
    [immutant.web :as web]
    [integrant.core :as ig]))

(defmethod ig/init-key ::server [_ {:keys [handler server-port]}]
  (let [server (web/run handler {:port server-port :host "0.0.0.0"})]
    (log/info (str "[SERVER] is listening on port " server-port))
    server))

(defmethod ig/halt-key! ::server [_ server]
  (log/info "[SERVER] is shutting down")
  (web/stop server))
