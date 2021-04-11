(ns com.ben-allred.audiophile.api.dev.core
  (:require
    [com.ben-allred.audiophile.common.utils.logger :as log]
    [integrant.core :as ig]
    [nrepl.server :as nrepl]))

(defmethod ig/init-key ::server [_ {:keys [nrepl-port]}]
  (log/info "[nREPL] is listening on port" nrepl-port)
  (nrepl/start-server :port nrepl-port))

(defmethod ig/halt-key! ::server [_ server]
  (log/info "[nREPL] is shutting down")
  (nrepl/stop-server server))
