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
    audiophile.backend.dev.accessors
    audiophile.backend.dev.handler))

(defonce system nil)

(defn system-init!
  ([routes daemons]
   (system-init! system routes daemons))
  ([sys routes daemons]
   (some-> sys ig/halt!)
   (binding [env*/*env* (merge env*/*env* (env/load-env [".env-common" ".env-dev"]))]
     (let [cfg (core/config "dev.edn" [:duct.profile/base :duct.profile/dev] routes)]
       (when (nil? sys)
         (core/create-topics! cfg))
       (core/build-system cfg daemons)))))

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
                             (some-> system ig/halt!)
                             nil))
  (reset-system! #{"api" "auth" "jobs" "ui"})
  (reset-system! #{"api"})
  (reset-system! #{"auth"})
  (reset-system! #{"jobs"})
  (reset-system! #{"ui"}))
