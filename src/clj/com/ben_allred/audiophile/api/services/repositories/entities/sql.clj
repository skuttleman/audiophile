(ns com.ben-allred.audiophile.api.services.repositories.entities.sql
  (:refer-clojure :exclude [cast count format max])
  (:require
    [com.ben-allred.audiophile.common.utils.logger :as log]
    [honeysql.core :as sql*]
    [honeysql.types :as ht]
    honeysql-postgres.format
    honeysql-postgres.helpers))

(def call ht/read-sql-call)

(defn cast [value type]
  (sql*/call :cast value type))

(defn count [field]
  (sql*/call :count field))

(defn max [field]
  (sql*/call :max field))

(defn coalesce [& args]
  (apply sql*/call :coalesce args))

(defn format
  ([query]
   (format query nil))
  ([query opts]
   (sql*/format query :quoting (:quoting opts :ansi))))

(defn raw [s]
  (sql*/raw s))
