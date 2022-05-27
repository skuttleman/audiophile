(ns audiophile.backend.infrastructure.db.models.sql
  (:refer-clojure :exclude [cast count format max])
  (:require
    [audiophile.common.core.utils.logger :as log]
    [honey.sql :as sql*]))

(defn cast [value type]
  [:cast value type])

(defn count [field]
  (keyword (str "%count." (name field))))

(defn max [field]
  [:max field])

(defn format
  ([query]
   (format query nil))
  ([query opts]
   (sql*/format query :quoting (:quoting opts :ansi))))
