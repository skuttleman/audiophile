(ns com.ben-allred.audiophile.api.dev.migrations
  (:require
    [clojure.java.io :as io]
    [clojure.string :as string]
    [com.ben-allred.audiophile.api.dev.protocols :as pdev]
    [com.ben-allred.audiophile.api.services.env :as env]
    [com.ben-allred.audiophile.api.services.repositories.core :as repos]
    [com.ben-allred.audiophile.common.utils.duct :as uduct]
    [duct.core :as duct]
    [duct.core.env :as env*]
    [integrant.core :as ig]
    [migratus.core :as migratus]
    com.ben-allred.audiophile.api.config.core
    com.ben-allred.audiophile.common.config.core))

(deftype Migrator [cfg]
  pdev/IMigrate
  (migrate [_]
    (migratus/migrate cfg))
  (rollback [_ n]
    (loop [rollbacks n]
      (when (pos? rollbacks)
        (migratus/rollback cfg)
        (recur (dec rollbacks)))))
  (create [_ name]
    (migratus/create cfg name)))

(defn migrate! [migrator]
  (pdev/migrate migrator))

(defn rollback!
  ([migrator]
   (rollback! migrator 1))
  ([migrator n]
   (pdev/rollback migrator n)))

(defn redo! [migrator]
  (rollback! migrator)
  (migrate! migrator))

(defn speedbump! [migrator]
  (migrate! migrator)
  (redo! migrator))

(defn seed! [transactor file]
  (let [seed-sql (some->> file io/resource slurp)]
    (repos/transact! transactor repos/execute! seed-sql)))

(defn create! [migrator name]
  (pdev/create migrator name))

(defmethod ig/init-key ::migrator [_ {:keys [datasource migrations-res-path]}]
  (->Migrator {:store                :database
               :migration-dir        (format "resources/%s/" migrations-res-path)
               :migration-table-name "db_migrations"
               :db                   {:datasource datasource}}))

(defn -main [command & [arg :as args]]
  (duct/load-hierarchy)
  (let [{migrator   ::migrator
         transactor ::repos/transactor
         :as        system} (binding [env*/*env* (merge env*/*env* (env/load-env [".env" ".env-dev" ".env-migrations"]))]
                              (-> "migrations.edn"
                                  duct/resource
                                  (duct/read-config uduct/readers)
                                  (duct/prep-config [:duct.profile/base
                                                     :duct.profile/dev
                                                     :duct.profile/migrations])
                                  (ig/init [::migrator ::repos/transactor])))]
    (try
      (case command
        "migrate" (migrate! migrator)
        "rollback" (rollback! migrator (Long/parseLong (or arg "1")))
        "speedbump" (speedbump! migrator)
        "redo" (redo! migrator)
        "create" (create! migrator (string/join "_" args))
        "seed" (seed! transactor (or arg "db/seed.sql"))
        (throw (ex-info (str "unknown command: " command) {:command command :args args})))
      (finally
        (ig/halt! system)))))

(comment
  (-main "create" "DESCRIPTION")
  (-main "migrate")
  (-main "redo"))
