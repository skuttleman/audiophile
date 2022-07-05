(ns audiophile.common.infrastructure.duct
  (:require
    [audiophile.common.core.utils.uuids :as uuids]
    [clojure.edn :as edn*]
    [duct.core :as duct]
    [duct.core.env :as env*]))

(def readers
  "custom readers for parsing duct config files"
  {'audiophile/merge      (partial reduce duct/merge-configs {})
   'audiophile/fk         (fn [[table col val]]
                            {:select [:id]
                             :from   [table]
                             :where  [:= col val]})
   'audiophile/uuid       (memoize (fn [_]
                                     (uuids/random)))
   'audiophile/event-type (fn [k]
                            (let [category (namespace k)
                                  name (name k)]
                              {:select [:id]
                               :from   [:event-types]
                               :where  [:and
                                        [:= :category category]
                                        [:= :name name]]}))})

(defmethod env*/coerce 'Edn [s _]
  (edn*/read-string s))
