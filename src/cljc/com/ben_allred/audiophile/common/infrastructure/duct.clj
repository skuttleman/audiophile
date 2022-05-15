(ns com.ben-allred.audiophile.common.infrastructure.duct
  (:require
    [clojure.edn :as edn*]
    [com.ben-allred.audiophile.backend.infrastructure.db.models.sql :as sql]
    [duct.core :as duct]
    [duct.core.env :as env*]))

(def readers
  "custom readers for parsing duct config files"
  {'audiophile/merge      (partial reduce duct/merge-configs {})
   'audiophile/fk         (fn [[table col val]]
                            {:select [:id]
                             :from   [table]
                             :where  [:= col val]})
   'audiophile/event-type (fn [k]
                            (let [category (namespace k)
                                  name (name k)]
                              {:select [:id]
                               :from   [:event-types]
                               :where  [:and
                                        [:= :category category]
                                        [:= :name name]]}))
   'audiophile/sql-call   sql/call})

(defmethod env*/coerce 'Edn [s _]
  (edn*/read-string s))
