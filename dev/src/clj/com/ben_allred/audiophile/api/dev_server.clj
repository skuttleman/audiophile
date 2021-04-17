(ns com.ben-allred.audiophile.api.dev-server
  (:require
    [com.ben-allred.audiophile.api.services.env :as env]
    [com.ben-allred.audiophile.common.utils.duct :as uduct]
    [com.ben-allred.audiophile.common.utils.logger :as log]
    [duct.core :as duct]
    [duct.core.env :as env*]
    [integrant.core :as ig]
    [nrepl.server :as nrepl]))

(defonce system nil)

(defn reset-sys!
  ([]
   (reset-sys! system))
  ([sys]
   (some-> sys ig/halt!)
   (binding [env*/*env* (merge env*/*env* (env/load-env [".env" ".env-dev"]))]
     (-> "dev.edn"
         duct/resource
         (duct/read-config uduct/readers)
         (duct/prep-config [:duct.profile/base :duct.profile/dev])
         (ig/init [:com.ben-allred.audiophile.api.core/server])))))

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

(comment
  (alter-var-root #'system reset-sys!))
