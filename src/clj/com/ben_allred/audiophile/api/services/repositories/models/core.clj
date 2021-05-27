(ns com.ben-allred.audiophile.api.services.repositories.models.core
  (:require
    [camel-snake-kebab.core :as csk]
    [com.ben-allred.audiophile.api.services.repositories.core :as repos]
    [com.ben-allred.audiophile.api.services.repositories.models.sql :as sql]
    [com.ben-allred.audiophile.common.utils.colls :as colls]
    [com.ben-allred.audiophile.common.utils.fns :as fns]
    [com.ben-allred.audiophile.common.utils.logger :as log]
    [integrant.core :as ig]))

(defmethod ig/init-key ::models [_ {:keys [tx]}]
  (reduce (fn [models row]
            (let [table (csk/->kebab-case-keyword (:table_name row))
                  column (csk/->kebab-case-keyword (:column_name row))]
              (update models
                      table
                      (fns/=> (update :fields (fnil conj #{}) column)
                              (cond->
                                (= "USER-DEFINED" (:data_type row))
                                (assoc-in [:casts column] (keyword (:udt_name row))))))))
          {}
          (repos/transact! tx repos/execute!
                           {:select [:table-name :column-name :data-type :udt-name]
                            :from   [:information-schema.columns]
                            :where  [:and
                                     [:= :table-schema "public"]
                                     [:not= :table-name "db_migrations"]]})))

(defmethod ig/init-key ::model [_ {:keys [models namespace table-name]}]
  (-> models
      (get table-name)
      (assoc :table table-name :namespace namespace)))

(defn ^:private from [{:keys [alias table]}]
  (cond-> table
    alias (vector alias)))

(defn ^:private ->field [{:keys [alias namespace table]}]
  (fn [field]
    [(keyword (str (name (or alias table)) "." (name field)))
     (str (name (or alias namespace)) "/" (name field))]))

(defn select-fields
  "Filter fields on a model before selection. Fields not on the model are ignored."
  [model field-set]
  (update model :fields (partial filter field-set)))

(defn remove-fields
  "Remove fields from a model before selection. Fields not on the model are ignored."
  [model field-set]
  (update model :fields (partial remove field-set)))

(defn select*
  "Generates a query for selecting from a database table"
  ([model clause]
   (assoc (select* model) :where clause))
  ([{:keys [fields] :as model}]
   {:select (mapv (->field model) fields)
    :from   [(from model)]}))

(defn ^:private valid-column? [{ns :namespace :keys [fields]} normalized-k ns-k]
  (and (contains? fields normalized-k)
       (or (nil? ns-k)
           (= (name ns) ns-k))))

(defn insert-into
  "Generates a query for inserting rows into a database table"
  [{:keys [casts fields table] :as model} input]
  {:insert-into table
   :values      (for [value (colls/force-sequential input)]
                  (into {}
                        (keep (fn [[k v]]
                                (let [k' (keyword (name k))
                                      cast (get casts k')]
                                  (when (valid-column? model k' (namespace k))
                                    [k' (cond-> v
                                          cast (-> name (sql/cast cast)))]))))
                        value))
   :returning   [(if (contains? fields :id) :id :*)]})

(defn join [query {:keys [_alias fields _namespace _table] :as model} on]
  (-> query
      (update :select into (map (->field model)) fields)
      (update :join
              (fnil conj [])
              (from model)
              on)))

(defn order-by [query & clauses]
  (update query :order-by (fnil into []) clauses))
