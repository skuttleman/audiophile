(ns com.ben-allred.audiophile.api.server
  (:gen-class)
  (:require
    [com.ben-allred.audiophile.api.services.env :as env]
    [com.ben-allred.audiophile.common.utils.duct :as uduct]
    [com.ben-allred.audiophile.common.utils.logger :as log]
    [duct.core :as duct]
    [duct.core.env :as env*]
    [immutant.web :as web]
    [integrant.core :as ig]
    com.ben-allred.audiophile.api.config.core
    com.ben-allred.audiophile.common.config.core))

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
