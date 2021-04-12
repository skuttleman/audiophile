(ns com.ben-allred.audiophile.api.server
  (:require
    [com.ben-allred.audiophile.api.services.env :as env]
    [duct.core :as duct]
    [duct.core.env :as env*]
    [integrant.core :as ig]))

(defn -main [& _]
  (duct/load-hierarchy)
  (let [system (binding [env*/*env* (merge env*/*env* (env/load-env [".env" ".env-prod"]))]
                 (-> "config.edn"
                     duct/resource
                     duct/read-config
                     (duct/prep-config [:duct.profile/prod])
                     (ig/init [:com.ben-allred.audiophile.api.core/server])))]
    (.addShutdownHook (Runtime/getRuntime)
                      (Thread. ^Runnable
                               (fn []
                                 (ig/halt! system))))
    (duct/await-daemons system)))
