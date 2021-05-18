(ns com.ben-allred.audiophile.common.utils.duct
  (:require
    [clojure.edn :as edn*]
    [com.ben-allred.audiophile.api.services.repositories.models.sql :as sql]
    [com.ben-allred.audiophile.common.utils.uuids :as uuids]
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
