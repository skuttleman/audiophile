(ns com.ben-allred.audiophile.api.dev.migrations
  (:require
    [clojure.java.io :as io]
    [clojure.string :as string]
    [com.ben-allred.audiophile.api.services.env :as env]
    [com.ben-allred.audiophile.api.services.repositories.core :as repos]
    [duct.core :as duct]
    [duct.core.env :as env*]
    [integrant.core :as ig]
    [migratus.core :as migratus]))

(defprotocol IMigrate
  (-migrate [this])
  (-rollback [this n])
  (-create [this name]))

(deftype Migrator [cfg]
  IMigrate
  (-migrate [_]
    (migratus/migrate cfg))
  (-rollback [_ n]
    (loop [rollbacks n]
      (when (pos? rollbacks)
        (migratus/rollback cfg)
        (recur (dec rollbacks)))))
  (-create [_ name]
    (migratus/create cfg name)))

(defn migrate! [migrator]
  (-migrate migrator))

(defn rollback!
  ([migrator]
   (rollback! migrator 1))
  ([migrator n]
   (-rollback migrator n)))

(defn redo! [migrator]
  (rollback! migrator)
  (migrate! migrator))

(defn speedbump! [migrator]
  (migrate! migrator)
  (redo! migrator))

(defn seed! [transactor file]
  (some->> file
           io/resource
           slurp
           (repos/transact! transactor repos/exec-raw!)))

(defn create! [migrator name]
  (-create migrator name))

(defmethod ig/init-key ::migrator [_ {:keys [datasource migrations-res-path]}]
  (->Migrator {:store                :database
               :migration-dir        (format "resources/%s/" migrations-res-path)
               :migration-table-name "db_migrations"
               :db                   {:datasource datasource}}))

(defn -main [command & [arg :as args]]
  (duct/load-hierarchy)
  (let [{migrator   ::migrator
         transactor ::repos/transactor
         :as        system} (binding [env*/*env* (merge env*/*env* (env/load-env [".env" ".env-dev"]))]
                              (-> "migrations.edn"
                                  duct/resource
                                  duct/read-config
                                  (duct/prep-config [:duct.profile/migrations])
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
  (-main "create" "create_users_table")
  (-main "migrate")
  (-main "redo"))
