(ns com.ben-allred.audiophile.backend.dev-server
  (:require
    [com.ben-allred.audiophile.backend.infrastructure.system.env :as env]
    [com.ben-allred.audiophile.common.core.utils.colls :as colls]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.common.infrastructure.duct :as uduct]
    [duct.core :as duct]
    [duct.core.env :as env*]
    [integrant.core :as ig]
    [nrepl.server :as nrepl]
    [ring.middleware.reload :as rel]
    com.ben-allred.audiophile.backend.dev.handler
    com.ben-allred.audiophile.backend.infrastructure.system.core))

(defonce system nil)

(def reload!
  (let [reload* (rel/wrap-reload identity {:dirs ["src/clj"
                                                  "src/cljc"
                                                  "test/clj"
                                                  "test/cljc"
                                                  "dev/src/clj"]})]
    (fn []
      (reload* nil)
      (require 'com.ben-allred.audiophile.backend.dev.handler :reload)
      (require 'com.ben-allred.audiophile.backend.api.repositories.common :reload)
      (require 'com.ben-allred.audiophile.backend.api.repositories.core :reload)
      (require 'com.ben-allred.audiophile.backend.infrastructure.db.core :reload)
      (require 'com.ben-allred.audiophile.backend.api.repositories.files.impl :reload)
      (require 'com.ben-allred.audiophile.backend.api.repositories.projects.impl :reload)
      (require 'com.ben-allred.audiophile.backend.api.repositories.teams.impl :reload)
      (require 'com.ben-allred.audiophile.backend.api.repositories.users.impl :reload)
      (require 'com.ben-allred.audiophile.backend.infrastructure.resources.s3 :reload)
      (require 'com.ben-allred.audiophile.common.api.navigation.core :reload)
      (require 'com.ben-allred.audiophile.common.core.serdes.core :reload))))

(defn reset-sys!
  ([]
   (reset-sys! system))
  ([sys]
   (some-> sys ig/halt!)
   (reload!)
   (binding [env*/*env* (merge env*/*env* (env/load-env [".env" ".env-dev"]))]
     (-> "dev.edn"
         duct/resource
         (duct/read-config uduct/readers)
         (duct/prep-config [:duct.profile/base :duct.profile/dev])
         (ig/init [:duct/daemon])))))

(defn -main [& _]
  (duct/load-hierarchy)
  (alter-var-root #'system reset-sys!)
  (let [nrepl-port (Long/parseLong (or (System/getenv "NREPL_PORT") "7000"))
        server (nrepl/start-server :port nrepl-port)]
    (log/info "[nREPL] is listening on port" nrepl-port)
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
  (do (alter-var-root #'system reset-sys!) nil))
