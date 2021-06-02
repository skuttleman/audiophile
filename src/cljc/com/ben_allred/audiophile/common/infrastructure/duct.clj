(ns com.ben-allred.audiophile.common.infrastructure.duct
  (:require
    [clojure.edn :as edn*]
    [com.ben-allred.audiophile.api.infrastructure.db.models.sql :as sql]
    [com.ben-allred.audiophile.common.core.utils.uuids :as uuids]
    [duct.core :as duct]
    [duct.core.env :as env*]))

(def readers
  "custom readers for parsing duct config files"
  {'audiophile/uuid-param (partial conj [uuids/regex])
   'audiophile/merge      (partial reduce duct/merge-configs {})
   'audiophile/fk         (fn [[table col val]]
                            {:select [:id]
                             :from   [table]
                             :where  [:= col val]})
   'audiophile/sql-call   sql/call})

(defmethod env*/coerce 'Edn [s _]
  (edn*/read-string s))