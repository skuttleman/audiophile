(ns com.ben-allred.audiophile.backend.core
  (:gen-class)
  (:require
    [com.ben-allred.audiophile.backend.infrastructure.system.env :as env]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.common.infrastructure.duct :as uduct]
    [duct.core :as duct]
    [duct.core.env :as env*]
    [integrant.core :as ig]
    com.ben-allred.audiophile.backend.infrastructure.system.core
    com.ben-allred.audiophile.common.infrastructure.system.core))

(defn -main [& _]
  (duct/load-hierarchy)
  (let [system (binding [env*/*env* (merge env*/*env* (env/load-env [".env" ".env-prod"]))]
                 (-> "config.edn"
                     duct/resource
                     (duct/read-config uduct/readers)
                     (duct/prep-config [:duct.profile/base :duct.profile/prod])
                     (ig/init [:duct/daemon])))]
    (.addShutdownHook (Runtime/getRuntime)
                      (Thread. ^Runnable
                               (fn []
                                 (ig/halt! system))))
    (duct/await-daemons system)))