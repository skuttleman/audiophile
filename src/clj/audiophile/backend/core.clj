(ns audiophile.backend.core
  (:gen-class)
  (:require
    [audiophile.backend.infrastructure.system.env :as env]
    [audiophile.common.core.utils.fns :as fns]
    [audiophile.common.core.utils.logger :as log]
    [audiophile.common.infrastructure.duct :as uduct]
    [duct.core :as duct]
    [duct.core.env :as env*]
    [integrant.core :as ig]
    audiophile.backend.infrastructure.system.core
    audiophile.common.infrastructure.system.core))

(defn ->refs [f prefix components]
  (into #{}
        (map (fns/=>> (str prefix) keyword f))
        components))

(defn -main [& components]
  (duct/load-hierarchy)
  (let [routes (->refs ig/ref "routes/table#" components)
        daemons (->refs identity "routes/daemon#" components)
        system (binding [env*/*env* (merge env*/*env* (env/load-env [".env-common" ".env-prod"]))]
                 (-> "config.edn"
                     duct/resource
                     (duct/read-config uduct/readers)
                     (assoc-in [:duct.profile/base [:duct.custom/merge :routes/table]] routes)
                     (duct/prep-config [:duct.profile/base :duct.profile/prod])
                     (ig/init (into [:duct/daemon] daemons))))]
    (.addShutdownHook (Runtime/getRuntime)
                      (Thread. ^Runnable
                               (fn []
                                 (ig/halt! system))))
    (duct/await-daemons system)))
