(ns com.ben-allred.audiophile.api.infrastructure.db.core
  (:require
    [com.ben-allred.audiophile.api.infrastructure.db.models.sql :as sql]
    [com.ben-allred.audiophile.api.app.repositories.protocols :as prepos]
    [com.ben-allred.audiophile.common.utils.logger :as log]
    [hikari-cp.core :as hikari]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as result-set])
  (:import
    (java.sql Date ResultSet)))

(deftype QueryFormatter []
  prepos/IFormatQuery
  (format [_ query]
    (sql/format query)))

(deftype RawFormatter []
  prepos/IFormatQuery
  (format [_ sql]
    (cond-> sql
      (not (vector? sql)) vector)))

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

(deftype Executor [conn ->builder-fn query-formatter]
  prepos/IExecute
  (execute! [_ query opts]
    (let [formatted (prepos/format query-formatter query)]
      (log/debug "[TX] - formatting:" query)
      (log/debug "[TX] - executing:" formatted)
      (jdbc/execute! conn formatted (assoc (:sql/opts opts)
                                           :builder-fn (->builder-fn opts))))))

(deftype Transactor [datasource opts ->executor]
  prepos/ITransact
  (transact! [_ f]
    (jdbc/transact datasource
                   (comp f ->executor)
                   opts)))

(defn query-formatter [_]
  (->QueryFormatter))

(defn raw-formatter [_]
  (->RawFormatter))

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

(defn ->executor [{:keys [->builder-fn query-formatter]}]
  (fn [conn]
    (->Executor conn ->builder-fn query-formatter)))

(defn transactor [{:keys [->executor datasource]}]
  (->Transactor datasource nil ->executor))

(defn cfg [{:keys [db-name host password port user]}]
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

(defn datasource [{:keys [spec]}]
  (hikari/make-datasource spec))

(defn datasource#close [datasource]
  (hikari/close-datasource datasource))
