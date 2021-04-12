(ns com.ben-allred.audiophile.api.services.repositories.core
  (:require
    [com.ben-allred.audiophile.api.services.repositories.protocols :as prepos]
    [hikari-cp.core :as hikari]
    [honeysql.core :as sql]
    [integrant.core :as ig]
    [next.jdbc :as jdbc]))

(deftype Executor [transactor conn]
  prepos/IExecute
  (execute! [_ query opts]
    (jdbc/execute! conn (sql/format query) opts))
  (exec-raw! [_ sql opts]
    (jdbc/execute! conn [sql] opts)))

(deftype Transactor [datasource opts]
  prepos/ITransact
  (transact! [this f]
    (jdbc/transact datasource
                   (fn [conn]
                     (f (->Executor this conn)))
                   opts)))

(defmethod ig/init-key ::transactor [_ {:keys [datasource]}]
  (->Transactor datasource nil))

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
