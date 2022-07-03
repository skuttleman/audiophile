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
    [kinsky.admin :as admin*]
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

(defn create-topics! [cfg]
  (let [[brokers & topics] (map (comp val (partial ig/find-derived-1 cfg))
                               [:env.kafka/brokers
                                :env.kafka.topic/tasks
                                :env.kafka.topic/workflows])]
    (with-open [admin (admin*/client {:bootstrap.servers brokers})]
      (let [existing? (into #{} (map :name) @(admin*/list-topics admin false))
            topics (remove
                     existing? topics)]
        (when (seq topics)
          (doseq [topic topics
                  :when (not (existing? topic))]
            (log/info "creating topic" topic)
            @(admin*/create-topic admin topic {:partitions         10
                                               :replication-factor 1})))
        (log/info (count topics) "topics creates")))))

(defn -main [& components]
  (duct/load-hierarchy)
  (let [routes (->refs ig/ref "routes/table#" components)
        daemons (->refs "routes/daemon#" components)
        system (binding [env*/*env* (merge env*/*env* (env/load-env [".env-common" ".env-prod"]))]
                 (let [cfg (config "config.edn" [:duct.profile/base :duct.profile/prod] routes)]
                   (create-topics! cfg)
                   (build-system cfg daemons)))]
    (.addShutdownHook (Runtime/getRuntime)
                      (Thread. ^Runnable
                               (fn []
                                 (ig/halt! system))))
    (duct/await-daemons system)))
