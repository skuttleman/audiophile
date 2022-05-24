(ns audiophile.backend.dev-server
  (:require
    [audiophile.backend.core :as core]
    [audiophile.backend.infrastructure.system.env :as env]
    [audiophile.common.core.utils.colls :as colls]
    [audiophile.common.core.utils.logger :as log]
    [audiophile.common.infrastructure.duct :as uduct]
    [duct.core :as duct]
    [duct.core.env :as env*]
    [integrant.core :as ig]
    [nrepl.server :as nrepl]
    [ring.middleware.reload :as rel]
    audiophile.backend.dev.accessors
    audiophile.backend.dev.handler))

(defonce system nil)

(def reload!
  (let [reload* (rel/wrap-reload identity {:dirs ["src/clj"
                                                  "src/cljc"
                                                  "test/clj"
                                                  "test/cljc"
                                                  "dev/src/clj"]})]
    (fn []
      (reload* nil)
      (require 'audiophile.backend.dev.accessors :reload)
      (require 'audiophile.backend.dev.handler :reload)
      (require 'audiophile.backend.api.repositories.common :reload)
      (require 'audiophile.backend.api.repositories.core :reload)
      (require 'audiophile.backend.api.repositories.events.impl :reload)
      (require 'audiophile.backend.api.repositories.files.impl :reload)
      (require 'audiophile.backend.api.repositories.projects.impl :reload)
      (require 'audiophile.backend.api.repositories.teams.impl :reload)
      (require 'audiophile.backend.api.repositories.users.impl :reload)
      (require 'audiophile.backend.infrastructure.resources.s3 :reload)
      (require 'audiophile.common.infrastructure.navigation.core :reload)
      (require 'audiophile.common.core.serdes.core :reload)
      (require 'audiophile.common.infrastructure.pubsub.memory :reload)
      (require 'audiophile.backend.infrastructure.db.core :reload))))

(defn system-init!
  ([routes daemons]
   (system-init! system routes daemons))
  ([sys routes daemons]
   (some-> sys ig/halt!)
   (reload!)
   (binding [env*/*env* (merge env*/*env* (env/load-env [".env-common" ".env-dev"]))]
     (-> "dev.edn"
         duct/resource
         (duct/read-config uduct/readers)
         (assoc-in [:duct.profile/base [:duct.custom/merge :routes/table]] routes)
         (duct/prep-config [:duct.profile/base :duct.profile/dev])
         (ig/init (into [:duct/daemon] daemons))))))

(defn reset-system! [components]
  (alter-var-root #'system
                  system-init!
                  (core/->refs ig/ref "routes/table#" components)
                  (core/->refs identity "routes/daemon#" components))
  nil)

(defn -main [& components]
  (duct/load-hierarchy)
  (reset-system! components)
  (let [nrepl-port (Long/parseLong (or (System/getenv "NREPL_PORT") "7000"))
        server (nrepl/start-server :port nrepl-port)]
    (log/with-ctx :nREPL
      (log/info "listening on port" nrepl-port))
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
  (reset-system! #{"api" "auth" "jobs" "ui"})
  (reset-system! #{"api"})
  (reset-system! #{"auth"})
  (reset-system! #{"jobs"})
  (reset-system! #{"ui"}))
