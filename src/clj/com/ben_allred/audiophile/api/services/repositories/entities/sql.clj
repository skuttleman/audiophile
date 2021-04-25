(ns com.ben-allred.audiophile.api.services.repositories.entities.sql
  (:refer-clojure :exclude [cast format])
  (:require
    [honeysql.core :as sql*]
    honeysql-postgres.format
    honeysql-postgres.helpers))

(defn cast [value type]
  (sql*/call :cast value type))

(defn format
  ([query]
   (format query nil))
  ([query opts]
   (sql*/format query :quoting (:quoting opts :ansi))))
