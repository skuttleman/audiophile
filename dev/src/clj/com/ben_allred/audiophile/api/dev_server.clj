(ns com.ben-allred.audiophile.api.dev-server
  (:require
    [com.ben-allred.audiophile.common.utils.logger :as log]
    [duct.core :as duct]
    [integrant.core :as ig]))

(defonce system nil)

(defn reset-sys!
  ([]
   (reset-sys! system))
  ([sys]
   (some-> sys ig/halt!)
   (-> "config.edn"
       duct/resource
       duct/read-config
       (duct/prep-config [:duct.profile/prod :duct.profile/dev])
       (ig/init [:com.ben-allred.audiophile.api.core/server
                 :com.ben-allred.audiophile.api.dev.core/server])
       (doto duct/await-daemons))))

(defn -main [& _]
  (duct/load-hierarchy)
  (alter-var-root #'system reset-sys!)
  (.addShutdownHook (Runtime/getRuntime)
                    (Thread. ^Runnable
                             (fn []
                               (ig/halt! system)))))
