(ns com.ben-allred.audiophile.backend.infrastructure.db.models.core
  (:require
    [camel-snake-kebab.core :as csk]
    [com.ben-allred.audiophile.backend.api.repositories.core :as repos]
    [com.ben-allred.audiophile.backend.infrastructure.db.models.sql :as sql]
    [com.ben-allred.audiophile.common.core.utils.colls :as colls]
    [com.ben-allred.audiophile.common.core.utils.fns :as fns]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.common.core.serdes.core :as serdes]
    [jsonista.core :as jsonista]
    [next.jdbc.result-set :as result-set])
  (:import
    (java.sql Date ResultSet)
    (org.postgresql.util PGobject)))

(extend-protocol jsonista/ReadValue
  PGobject
  (-read-value [this mapper]
    (.readValue mapper (str this) ^Class Object)))

(deftype Builder [cols col-cnt collect! ->row! rs]
  result-set/RowBuilder
  (->row [_] (transient {}))
  (column-count [_] col-cnt)
  (with-column [_ row i]
    (let [k (nth cols (dec i))
          v (result-set/read-column-by-index (.getObject ^ResultSet rs ^Integer i) meta i)]
      (->row! row k (cond-> v (instance? Date v) .toLocalDate))))
  (row! [_ row] (persistent! row))

  result-set/ResultSetBuilder
  (->rs [_] (transient []))
  (with-row [_ mrs row] (collect! mrs row))
  (rs! [_ mrs] (persistent! mrs)))

(defn ->builder-fn [_]
  (fn [{:keys [model-fn result-xform]}]
    (let [xform (or result-xform identity)
          model-fn (cond->> (fn [[k v]]
                              [(keyword k) v])
                            model-fn (comp model-fn))
          ->row! (fn [t k v]
                   (conj! t (model-fn [k v])))]
      (fn [^ResultSet rs _opts]
        (let [meta (.getMetaData rs)
              col-cnt (.getColumnCount meta)
              cols (mapv (fn [^Integer i] (keyword (.getColumnLabel meta (inc i))))
                         (range col-cnt))]
          (->Builder cols col-cnt (xform conj!) ->row! rs))))))

(defn models [{:keys [tx]}]
  (reduce (fn [models {table :table_name column :column_name type :data_type name :udt_name}]
            (let [table (csk/->kebab-case-keyword table)
                  column (csk/->kebab-case-keyword column)]
              (update models
                      table
                      (fns/=> (update :fields (fnil conj #{}) column)
                              (cond->
                                (= "USER-DEFINED" type)
                                (assoc-in [:casts column] (keyword name))

                                (contains? #{"jsonb"} type)
                                (assoc-in [:casts column] (keyword type)))))))
          {}
          (repos/transact! tx repos/execute!
                           {:select [:table-name :column-name :data-type :udt-name]
                            :from   [:information-schema.columns]
                            :where  [:and
                                     [:= :table-schema "public"]
                                     [:not= :table-name "db_migrations"]]})))

(defn model [{:keys [models namespace table-name]}]
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

(defn select-by-id* [{:keys [alias table] :as model} id]
  (let [primary-key (keyword (str (name (or alias table)) ".id"))]
    (select* model [:= primary-key id])))

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
                                      cast (get casts k')
                                      pre-cast (when cast
                                                 (case cast
                                                   :jsonb (partial serdes/serialize (serdes/json {}))
                                                   name))]
                                  (when (valid-column? model k' (namespace k))
                                    [k' (cond-> v
                                          pre-cast (-> pre-cast (sql/cast cast)))]))))
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
