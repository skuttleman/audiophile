(ns com.ben-allred.audiophile.backend.dev.uml
  (:require [com.ben-allred.audiophile.backend.app.repositories.core :as repos]
            [com.ben-allred.audiophile.common.core.utils.maps :as maps]
            [clojure.string :as string]))

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

(defn ^:private entity [[table {:keys [id fields]}]]
  (concat [(format "entity \"%s\" as %s {" table table)]
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
  (->> "SELECT table_name, column_name, data_type, column_default, is_nullable
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name <> 'db_migrations'"
       (repos/transact! tx repos/execute!)
       (reduce (fn [entities {table    :table_name
                              column   :column_name
                              type     :data_type
                              default  :column_default
                              nilable? :is_nullable}]
                 (let [field (maps/->m [:name column]
                                       [:nilable? ({"YES" true "NO" false} nilable?)]
                                       type
                                       default)]
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
