(ns audiophile.backend.dev.uml
  (:require
    [clojure.string :as string]
    [audiophile.backend.infrastructure.repositories.core :as repos]
    [audiophile.common.core.utils.maps :as maps]))

(defn ^:private indent [x]
  (str "  " x))

(defn ^:private header []
  ["@startuml"
   "hide circle"
   "skinparam linetype ortho"
   ""])

(defn ^:private field [{:keys [default name nilable? type]}]
  (let [fk? (string/ends-with? name "_id")]
    (indent (str (when-not nilable? "* ")
                 name
                 " : "
                 type
                 (when (or default fk?)
                   " ")
                 (when default "<<generated>>")
                 (when fk? "<<FK>>")))))

(defn ^:private entity [[table {:keys [id fields view?]}]]
  (concat [(format "entity \"%s\" as %s {"
                   (cond-> table
                     view? (str " *VIEW*"))
                   table)]
          (when id
            [(field id) (indent "--")])
          (map field fields)
          ["}" ""]))

(defn ^:private relations [schema]
  (for [[table {:keys [fields]}] schema
        {:keys [name]} fields
        :let [relation (when (string/ends-with? name "_id")
                         (str (subs name 0 (- (count name) 3)) "s"))]
        :when (contains? schema relation)]
    (str relation " ..o " table)))

(defn ^:private trailer []
  ["@enduml"
   ""])

(defn ^:private select-schema [tx]
  (->> "SELECT c.table_name, c.column_name, c.data_type,
               c.column_default, c.is_nullable, t.table_type
        FROM information_schema.columns c
        JOIN information_schema.tables t
          ON t.table_catalog = c.table_catalog
          AND t.table_schema = c.table_schema
          AND t.table_name = c.table_name
        WHERE c.table_schema = 'public'
          AND c.table_name <> 'db_migrations'"
       (repos/transact! tx repos/execute!)
       (reduce (fn [entities {table      :table_name
                              column     :column_name
                              type       :data_type
                              default    :column_default
                              nilable?   :is_nullable
                              table-type :table_type}]
                 (let [field (maps/->m {:name     column
                                        :nilable? ({"YES" true "NO" false} nilable?)}
                                       type
                                       default)
                       entities (update entities table assoc :view? (boolean ({"VIEW" true} table-type)))]
                   (if (= "id" column)
                     (update entities table assoc :id field)
                     (update-in entities [table :fields] (fnil conj []) field))))
               {})))

(defn generate! [tx output]
  (let [schema (select-schema tx)]
    (->> (concat (header)
                 (mapcat entity schema)
                 (relations schema)
                 [""]
                 (trailer))
         (string/join "\n")
         (spit output))))
