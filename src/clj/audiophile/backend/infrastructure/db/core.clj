(ns audiophile.backend.infrastructure.db.core
  (:require
    [audiophile.backend.api.repositories.protocols :as prepos]
    [audiophile.backend.infrastructure.db.models.sql :as sql]
    [audiophile.common.infrastructure.http.protocols :as phttp]
    [audiophile.common.core.utils.logger :as log]
    [hikari-cp.core :as hikari]
    [next.jdbc :as jdbc])
  (:import
    (java.sql Timestamp)))

(deftype QueryFormatter [opts]
  prepos/IFormatQuery
  (format [_ query]
    (sql/format query opts)))

(deftype RawFormatter []
  prepos/IFormatQuery
  (format [_ sql]
    (cond-> sql
      (not (vector? sql)) vector)))

(deftype Executor [conn ->builder-fn query-formatter]
  prepos/IExecute
  (execute! [this query opts]
    (let [formatted (mapv #(cond-> % (inst? %) (-> .getTime Timestamp.)) (prepos/format query-formatter query))]
      (log/with-ctx [this :DB]
        (log/debug query)
        (log/info (first formatted)))
      (jdbc/execute! conn formatted (assoc (:sql/opts opts)
                                           :builder-fn (->builder-fn opts))))))

(deftype Transactor [datasource opts ->executor]
  prepos/ITransact
  (transact! [_ f]
    (jdbc/transact datasource
                   (comp f ->executor)
                   opts))

  phttp/ICheckHealth
  (display-name [_]
    ::Transactor)
  (healthy? [_]
    (let [conn (.getConnection datasource)]
      (try
        (not (.isClosed conn))
        (catch Throwable _
          false)
        (finally (.close conn)))))
  (details [_]
    nil))

(defn query-formatter
  "Constructor for [[QueryFormatter]] to convert query maps into a prepared SQL statement."
  [{:keys [format-opts]}]
  (->QueryFormatter format-opts))

(defn raw-formatter
  "Constructor for [[RawFormatter]] used to query with raw SQL directly."
  [_]
  (->RawFormatter))

(defn ->executor
  "Factor function for constructing an [[Executor]] used to run individual queries inside a transaction."
  [{:keys [->builder-fn query-formatter]}]
  (fn [conn]
    (->Executor conn ->builder-fn query-formatter)))

(defn transactor
  "Constructor for [[Transactor]] used for running operations inside a transaction."
  [{:keys [->executor datasource]}]
  (->Transactor datasource nil ->executor))

(defn cfg
  "Generates configuration used for [[javax.sql.DataSource]]"
  [{:keys [db-name host password port user]}]
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

(defn datasource
  "Constructor for a [[javax.sql.DataSource]]."
  [{:keys [spec]}]
  (hikari/make-datasource spec))

(defn datasource#close
  "Closes the [[javax.sql.DataSource]]."
  [datasource]
  (hikari/close-datasource datasource))
