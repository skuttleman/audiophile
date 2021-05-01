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

(defn select*
  "Generates a query for selecting from a database table"
  ([entity clause]
   (assoc (select* entity) :where clause))
  ([{:keys [alias fields namespace table] :as entity}]
   {:select (mapv (fn [field]
                    [(keyword (str (name table) "." (name field)))
                     (str (name namespace) "/" (name field))])
                  fields)
    :from   [(if alias
               [table alias]
               table)]
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
