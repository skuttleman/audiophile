(ns com.ben-allred.audiophile.backend.api.repositories.common
  (:require
    [com.ben-allred.audiophile.backend.api.repositories.core :as repos]
    [com.ben-allred.audiophile.backend.api.repositories.protocols :as prepos]
    [com.ben-allred.audiophile.backend.domain.interactors.core :as int]
    [com.ben-allred.audiophile.backend.infrastructure.pubsub.core :as ps]
    [com.ben-allred.audiophile.common.core.serdes.core :as serdes]
    [com.ben-allred.audiophile.common.core.utils.fns :as fns]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]))

(defn ->model-fn [model]
  (fn [[k v]]
    (let [k' (keyword (name k))
          cast-fn (if-let [cast (get-in model [:casts k'])]
                    (case cast
                      :jsonb (partial serdes/deserialize (serdes/json {}))
                      :numrange #(some->> % .getValue (serdes/deserialize (serdes/edn {})))
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
  [repo opts f & args]
  `(let [{on-success# :on-success request-id# :request/id :as opts#} ~opts]
     (future
       (try
         (let [result# (repos/transact! ~repo ~f ~@args opts#)]
           (when on-success#
             (repos/transact! ~repo on-success# result# opts#))
           result#)
         (catch Throwable ex#
           (if request-id#
             (do (log/error ex# "command failed" request-id#)
                 (repos/transact! ~repo
                                  int/command-failed!
                                  request-id#
                                  (update opts# :error/reason (fns/=> (or (.getMessage ex#))))))
             (log/error ex# "processing failed")))))))

(defn msg-handler
  ([predicate log handler]
   (msg-handler predicate log handler nil))
  ([predicate log handler on-error]
   (fn [{[msg-id] :msg :as msg}]
     (if-not (predicate msg)
       (log/debug "skipping msg due to predicate" msg)
       (try
         (log/info log msg-id)
         (handler msg)
         (catch Throwable ex
           (log/error ex "failed: " log msg)
           (when on-error
             (on-error ex msg))))))))

(defn command-handler [pubsub predicate log handler]
  (msg-handler predicate
               log
               handler
               (fn [ex {[_ command ctx] :msg}]
                 (try
                   (ps/command-failed! pubsub
                                       (:request/id ctx)
                                       (assoc ctx
                                              :error/command (:command/type command)
                                              :error/reason (.getMessage ex)))
                   (catch Throwable ex
                     (log/error ex "failed to emit command/failed"))))))
