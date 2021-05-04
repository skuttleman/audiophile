(ns com.ben-allred.audiophile.api.services.repositories.entities.core
  (:require
    [camel-snake-kebab.core :as csk]
    [com.ben-allred.audiophile.api.services.repositories.core :as repos]
    [com.ben-allred.audiophile.api.services.repositories.entities.sql :as sql]
    [com.ben-allred.audiophile.common.utils.colls :as colls]
    [com.ben-allred.audiophile.common.utils.logger :as log]
    [integrant.core :as ig]))

(defmethod ig/init-key ::entities [_ {:keys [tx]}]
  (reduce (fn [entities row]
            (let [table (csk/->kebab-case-keyword (:table_name row))
                  column (csk/->kebab-case-keyword (:column_name row))]
              (update entities
                      table
                      (fn [entity]
                        (-> entity
                            (update :fields
                                    (fnil conj #{})
                                    column)
                            (cond->
                              (= "USER-DEFINED" (:data_type row))
                              (assoc-in [:casts column] (keyword (:udt_name row)))))))))
          {}
          (repos/transact! tx (fn [executor _]
                                (repos/execute! executor
                                                {:select [:table-name :column-name :data-type :udt-name]
                                                 :from   [:information-schema.columns]
                                                 :where  [:and
                                                          [:= :table-schema "public"]
                                                          [:not= :table-name "db_migrations"]]})))))

(defmethod ig/init-key ::entity [_ {:keys [entities namespace table-name]}]
  (-> entities
      (get table-name)
      (assoc :table table-name :namespace namespace)))

(defn ^:private from [{:keys [alias table]}]
  (cond-> table
    alias (vector alias)))

(defn ^:private ->field [{:keys [alias namespace table]}]
  (fn [field]
    [(keyword (str (name (or alias table)) "." (name field)))
     (str (name (or alias namespace)) "/" (name field))]))

(defn select-fields [entity field-set]
  (update entity :fields (partial filter field-set)))

(defn remove-fields [entity field-set]
  (update entity :fields (partial remove field-set)))

(defn select*
  "Generates a query for selecting from a database table"
  ([entity clause]
   (assoc (select* entity) :where clause))
  ([{:keys [fields] :as entity}]
   {:select (mapv (->field entity) fields)
    :from   [(from entity)]
    :entity entity}))

(defn insert-into
  "Generates a query for inserting rows into a database table"
  [{:keys [casts fields table]} input]
  {:insert-into table
   :values      (for [value (colls/force-sequential input)]
                  (into {}
                        (keep (fn [[k v]]
                                (let [k' (keyword (name k))
                                      cast (get casts k')]
                                  (when (contains? fields k')
                                    [k' (cond-> v
                                          cast (-> name (sql/cast cast)))]))))
                        value))
   :returning   [(if (contains? fields :id) :id :*)]})

(defn join [query {:keys [fields] :as entity} on]
  (-> query
      (update :select into (map (->field entity)) fields)
      (update :join
              (fnil conj [])
              (from entity)
              on)))

(defn order-by [query & clauses]
  (update query :order-by (fnil into []) clauses))
