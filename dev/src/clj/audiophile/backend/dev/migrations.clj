(ns audiophile.backend.dev.migrations
  (:require
    [clojure.java.io :as io]
    [clojure.string :as string]
    [audiophile.backend.api.repositories.core :as repos]
    [audiophile.backend.dev.protocols :as pdev]
    [audiophile.backend.dev.uml :as uml]
    [audiophile.backend.infrastructure.system.env :as env]
    [audiophile.common.infrastructure.duct :as uduct]
    [duct.core :as duct]
    [duct.core.env :as env*]
    [integrant.core :as ig]
    [migratus.core :as migratus]
    audiophile.backend.infrastructure.system.core
    audiophile.common.infrastructure.system.core))

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

(defn create-migrator [datasource]
  (->Migrator {:store                :database
               :migration-dir        "resources/db/migrations/"
               :migration-table-name "db_migrations"
               :db                   {:datasource datasource}}))

(defn migrate! [migrator]
  (pdev/migrate migrator))

(defn rollback!
  ([migrator]
   (rollback! migrator 1))
  ([migrator n]
   (pdev/rollback migrator n)))

(defn seed! [transactor file]
  (let [seed-sql (some->> file io/resource slurp)]
    (repos/transact! transactor repos/execute! seed-sql)))

(defn create! [migrator name]
  (pdev/create migrator name))

(defmethod ig/init-key :audiophile.migrations/migrator [_ {:keys [datasource]}]
  (create-migrator datasource))

(defmacro ^:private with-migrator [sys & body]
  `(let [system# (binding [env*/*env* (merge env*/*env* (env/load-env [".env-common" ".env-dev" ".env-migrations"]))]
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
  ([_]
   (main* :rollback "1"))
  ([_ n]
   (with-migrator {:audiophile.migrations/keys [migrator]}
     (rollback! migrator (Long/parseLong n)))))

(defmethod main* :speedbump
  [_]
  (with-migrator {:audiophile.migrations/keys [migrator]}
    (migrate! migrator)
    (rollback! migrator)
    (migrate! migrator)))

(defmethod main* :redo
  [_]
  (with-migrator {:audiophile.migrations/keys [migrator]}
    (rollback! migrator)
    (migrate! migrator)))

(defmethod main* :create
  [_ & description]
  (with-migrator {:audiophile.migrations/keys [migrator]}
    (create! migrator (string/join "_" description))))

(defmethod main* :seed
  ([_]
   (main* :seed "db/seed.sql"))
  ([_ seed-file]
   (with-migrator {transactor [:audiophile.repositories/transactor :component/health]}
     (seed! transactor seed-file))))

(defmethod main* :generate-erd
  ([_]
   (main* :generate-erd "docs/diagrams/erd.puml"))
  ([_ output]
   (with-migrator {transactor [:audiophile.repositories/transactor :component/health]}
     (uml/generate! transactor output))))

(defn -main [command & args]
  (duct/load-hierarchy)
  (apply main* command args))

(comment
  (-main :create "track_comments")
  (-main :migrate)
  (-main :rollback)
  (-main :redo)
  (-main :speedbump)

  (-main :generate-erd))
