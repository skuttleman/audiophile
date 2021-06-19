(ns com.ben-allred.audiophile.backend.dev.migrations
  (:require
    [clojure.java.io :as io]
    [clojure.string :as string]
    [com.ben-allred.audiophile.backend.api.repositories.core :as repos]
    [com.ben-allred.audiophile.backend.dev.protocols :as pdev]
    [com.ben-allred.audiophile.backend.dev.uml :as uml]
    [com.ben-allred.audiophile.backend.infrastructure.system.env :as env]
    [com.ben-allred.audiophile.common.infrastructure.duct :as uduct]
    [duct.core :as duct]
    [duct.core.env :as env*]
    [integrant.core :as ig]
    [migratus.core :as migratus]
    com.ben-allred.audiophile.backend.infrastructure.system.core
    com.ben-allred.audiophile.common.infrastructure.system.core))

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

(defmethod ig/init-key :audiophile.migrations/migrator [_ {:keys [datasource migrations-res-path]}]
  (->Migrator {:store                :database
               :migration-dir        (format "resources/%s/" migrations-res-path)
               :migration-table-name "db_migrations"
               :db                   {:datasource datasource}}))

(defmacro ^:private with-migrator [sys & body]
  `(let [system# (binding [env*/*env* (merge env*/*env* (env/load-env [".env" ".env-dev" ".env-migrations"]))]
                              (-> "migrations.edn"
                                  duct/resource
                                  (duct/read-config uduct/readers)
                                  (duct/prep-config [:duct.profile/base
                                                     :duct.profile/dev
                                                     :duct.profile/migrations])
                                  (ig/init [:audiophile.migrations/migrator :audiophile.repositories/transactor])))
         ~sys system#]
     (try
       ~@body
       (finally
         (ig/halt! system#)))))

(defmulti ^:private main* (fn [command & _] (keyword command)))

(defmethod main* :migrate
  [_]
  (with-migrator {:audiophile.migrations/keys [migrator]}
    (migrate! migrator)))

(defmethod main* :rollback
  [_ & [n]]
  (with-migrator {:audiophile.migrations/keys [migrator]}
    (rollback! migrator (Long/parseLong (or n "1")))))

(defmethod main* :speedbump
  [_]
  (with-migrator {:audiophile.migrations/keys [migrator]}
    (speedbump! migrator)))

(defmethod main* :redo
  [_]
  (with-migrator {:audiophile.migrations/keys [migrator]}
    (redo! migrator)))

(defmethod main* :create
  [_ & description]
  (with-migrator {:audiophile.migrations/keys [migrator]}
    (create! migrator (string/join "_" description))))

(defmethod main* :seed
  [_ & [seed-file]]
  (with-migrator {:audiophile.repositories/keys [transactor]}
    (seed! transactor (or seed-file "db/seed.sql"))))

(defmethod main* :generate-erd
  [_ & [output]]
  (with-migrator {:audiophile.repositories/keys [transactor]}
    (uml/generate! transactor (or output "resources/db/erd.puml"))))

(defn -main [command & args]
  (duct/load-hierarchy)
  (apply main* command args))

(comment
  (-main :create "user_events_view")
  (-main :migrate)
  (-main :rollback)

  (-main :generate-erd))
