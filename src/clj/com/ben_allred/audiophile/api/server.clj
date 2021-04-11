(ns com.ben-allred.audiophile.api.server
  (:require
    [com.ben-allred.audiophile.common.utils.logger :as log]
    [duct.core :as duct]
    [integrant.core :as ig]
    [clojure.java.io :as io]))

(defn -main [& _]
  (duct/load-hierarchy)
  (let [system (-> "config.edn"
                   duct/resource
                   duct/read-config
                   (duct/prep-config [:duct.profile/prod])
                   (ig/init [:com.ben-allred.audiophile.api.core/server])
                   (doto duct/await-daemons))]
    (.addShutdownHook (Runtime/getRuntime)
                      (Thread. ^Runnable
                               (fn []
                                 (ig/halt! system))))))
