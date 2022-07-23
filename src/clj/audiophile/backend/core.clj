(ns audiophile.backend.core
  (:gen-class)
  (:require
    [audiophile.backend.infrastructure.system.env :as env]
    [audiophile.common.core.utils.fns :as fns]
    [audiophile.common.infrastructure.duct :as uduct]
    [duct.core :as duct]
    [duct.core.env :as env*]
    [integrant.core :as ig]
    audiophile.backend.infrastructure.system.core
    audiophile.common.infrastructure.system.core))

(defn ->refs
  ([prefix components]
   (->refs identity prefix components))
  ([f prefix components]
   (into #{}
         (map (fns/=>> (str prefix) keyword f))
         components)))

(defn config [file profiles routes]
  (-> file
      duct/resource
      (duct/read-config uduct/readers)
      (assoc-in [:duct.profile/base [:duct.custom/merge :routes/table]] routes)
      (duct/prep-config profiles)))

(defn build-system [cfg daemons]
  (ig/init cfg (into [:duct/daemon] daemons)))

(defn -main [& components]
  (duct/load-hierarchy)
  (let [routes (->refs ig/ref "routes/table#" components)
        daemons (->refs "routes/daemon#" components)
        system (binding [env*/*env* (merge env*/*env* (env/load-env [".env-common" ".env-prod"]))]
                 (-> "config.edn"
                     (config [:duct.profile/base :duct.profile/prod] routes)
                     (build-system daemons)))]
    (.addShutdownHook (Runtime/getRuntime)
                      (Thread. ^Runnable
                               (fn []
                                 (ig/halt! system))))
    (duct/await-daemons system)))

(comment
  (require 'audiophile.backend.core :reload-all))
