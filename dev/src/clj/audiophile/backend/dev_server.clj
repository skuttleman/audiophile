(ns audiophile.backend.dev-server
  (:require
    [audiophile.backend.core :as core]
    [audiophile.backend.infrastructure.system.env :as env]
    [audiophile.common.core.utils.colls :as colls]
    [audiophile.common.core.utils.logger :as log]
    [duct.core :as duct]
    [duct.core.env :as env*]
    [integrant.core :as ig]
    [nrepl.server :as nrepl]
    audiophile.backend.dev.handler))

(defonce system nil)

(defn system-init!
  ([routes daemons]
   (system-init! system routes daemons))
  ([sys routes daemons]
   (when sys
     (ig/halt! sys)
     (log/info "halt completed")
     (Thread/sleep 1000))
   (binding [env*/*env* (merge env*/*env* (env/load-env [".env-common" ".env-dev"]))]
     (-> "dev.edn"
         (core/config [:duct.profile/base :duct.profile/dev] routes)
         (core/build-system daemons)))))

(defn reset-system! [components]
  (alter-var-root #'system
                  system-init!
                  (core/->refs ig/ref "routes/table#" components)
                  (core/->refs "routes/daemon#" components))
  nil)

(defn -main [& components]
  (let [nrepl-port (Long/parseLong (or (System/getenv "NREPL_PORT") "7000"))
        server (nrepl/start-server :bind "0.0.0.0" :port nrepl-port)]
    (log/with-ctx :nREPL
      (log/info "listening on port" nrepl-port))
    (duct/load-hierarchy)
    (reset-system! components)
    (.addShutdownHook (Runtime/getRuntime)
                      (Thread. ^Runnable
                               (fn []
                                 (log/info "[nREPL] is shutting down")
                                 (nrepl/stop-server server)
                                 (ig/halt! system))))
    (duct/await-daemons system)))

(defn component [k]
  (second (colls/only! (ig/find-derived system k))))

(comment
  (alter-var-root #'system (fn [sys]
                             (some-> sys ig/halt!)
                             nil))
  (reset-system! #{"api" "auth" "tasks" "ui" "wf"}))
