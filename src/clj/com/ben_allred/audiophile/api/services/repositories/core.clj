(ns com.ben-allred.audiophile.api.services.repositories.core
  (:require
    [com.ben-allred.audiophile.api.services.repositories.protocols :as prepos]
    [com.ben-allred.audiophile.common.utils.logger :as log]
    [hikari-cp.core :as hikari]
    [honeysql.core :as sql]
    [integrant.core :as ig]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as result-set])
  (:import
    (java.sql Date ResultSet)))

(defn ^:private exec* [conn psql opts]
  (jdbc/execute! conn psql opts))

(deftype QueryFormatter []
  prepos/IFormatQuery
  (format [_ query]
    (sql/format query :quoting :ansi)))

(defmethod ig/init-key ::query-formatter [_ _]
  (->QueryFormatter))

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

(defmethod ig/init-key ::->builder-fn [_ _]
  (fn [{:keys [entity-fn result-xform]}]
    (let [xform (or result-xform identity)
          entity-fn (cond->> (fn [[k v]]
                               [(keyword k) v])
                             entity-fn (comp entity-fn))
          ->row! (fn [t k v]
                   (conj! t (entity-fn [k v])))]
      (fn [^ResultSet rs _opts]
        (let [meta (.getMetaData rs)
              col-cnt (.getColumnCount meta)
              cols (mapv (fn [^Integer i] (keyword (.getColumnLabel meta (inc i))))
                         (range col-cnt))]
          (->Builder cols col-cnt (xform conj!) ->row! rs))))))

(deftype Executor [conn ->builder-fn query-formatter]
  prepos/IExecute
  (exec-raw! [_ sql opts]
    (let [sql-params (cond-> sql
                       (not (vector? sql)) vector)]
      (log/debug "[TX] - executing:" sql-params)
      (exec* conn sql-params (assoc (:sql/opts opts)
                                    :builder-fn (->builder-fn opts)))))
  (execute! [this query opts]
    (log/debug "[TX] - formatting:" query)
    (prepos/exec-raw! this
                      (prepos/format query-formatter query)
                      opts)))

(defmethod ig/init-key ::->executor [_ {:keys [->builder-fn query-formatter]}]
  (fn [conn] (->Executor conn ->builder-fn query-formatter)))

(deftype Transactor [datasource opts ->executor]
  prepos/ITransact
  (transact! [_ f]
    (jdbc/transact datasource
                   (fn [conn]
                     (f (->executor conn)))
                   opts)))

(defmethod ig/init-key ::transactor [_ {:keys [->executor datasource]}]
  (->Transactor datasource nil ->executor))

(defmethod ig/init-key ::cfg [_ {:keys [db-name host password port user]}]
  {:auto-commit           true
   :read-only             false
   :connection-timeout    10000
   :validation-timeout    5000
   :idle-timeout          600000
   :max-lifetime          1800000
   :minimum-idle          5
   :maximum-pool-size     10
   :pool-name             "db-pool"
   :adapter               "postgresql"
   :datasource-class-name "org.postgresql.ds.PGPoolingDataSource"
   :username              user
   :password              password
   :database-name         db-name
   :server-name           host
   :port-number           port
   :register-mbeans       false})

(defmethod ig/init-key ::datasource [_ {:keys [spec]}]
  (hikari/make-datasource spec))

(defmethod ig/halt-key! ::datasource [_ datasource]
  (hikari/close-datasource datasource))

(defn execute!
  ([executor query]
   (execute! executor query nil))
  ([executor query opts]
   (prepos/execute! executor query opts)))

(defn exec-raw!
  ([executor sql]
   (exec-raw! executor sql nil))
  ([executor sql opts]
   (prepos/exec-raw! executor sql opts)))

(defn transact!
  ([transactor f]
   (prepos/transact! transactor f))
  ([transactor f & args]
   (prepos/transact! transactor #(apply f % args))))
