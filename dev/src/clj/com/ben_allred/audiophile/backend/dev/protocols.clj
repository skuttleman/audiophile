(ns com.ben-allred.audiophile.backend.dev.protocols)

(defprotocol IMigrate
  "Manage DB Migrations"
  (migrate [this] "run migrations")
  (rollback [this n] "rollback n migrations")
  (create [this name] "create a new migration"))
