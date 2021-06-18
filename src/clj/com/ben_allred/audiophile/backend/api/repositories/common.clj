(ns com.ben-allred.audiophile.backend.api.repositories.common
  (:require
    [com.ben-allred.audiophile.backend.api.repositories.core :as repos]
    [com.ben-allred.audiophile.backend.api.repositories.protocols :as prepos]
    [com.ben-allred.audiophile.backend.domain.interactors.protocols :as pint]
    [com.ben-allred.audiophile.common.core.serdes.core :as serdes]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]))

(defn ->model-fn [model]
  (fn [[k v]]
    (let [k' (keyword (name k))
          cast-fn (if-let [cast (get-in model [:casts k'])]
                    (case cast
                      :jsonb (partial serdes/deserialize (serdes/json {}))
                      keyword)
                    identity)]
      [k (cast-fn v)])))

(deftype Repository [tx ->executor]
  prepos/ITransact
  (transact! [_ f]
    (repos/transact! tx (comp f ->executor))))

(defn repo
  "Constructor for [[Repository]] used for running processes inside a transaction."
  [{:keys [->executor tx]}]
  (->Repository tx ->executor))

(deftype KVStore [client stream-serde]
  prepos/IKVStore
  (uri [_ key opts]
    (prepos/uri client key opts))
  (get [_ key opts]
    (serdes/deserialize stream-serde (prepos/get client key opts)))
  (put! [_ key value opts]
    (prepos/put! client key (serdes/serialize stream-serde value) opts)))

(defn store
  "Constructor for [[KVStore]] used to store and retrieve binary objects."
  [{:keys [client stream-serde]}]
  (->KVStore client stream-serde))

(defmacro command!
  "Utility for expressing async commands with error handling."
  [repo opts & body]
  `(let [opts# ~opts]
     (future
       (try
         ~@body
         (catch Throwable ex#
           (if-let [request-id# (:request/id opts#)]
             (do (log/error ex# "command failed" request-id#)
                 (try
                   (repos/transact! ~repo pint/command-failed! request-id# (assoc opts# :error/reason (.getMessage ex#)))
                   (catch Throwable ex#
                     (log/fatal ex# "command/failed not emitted."))))
             (log/error ex# "processing failed")))))))
