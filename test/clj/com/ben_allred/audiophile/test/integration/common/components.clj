(ns com.ben-allred.audiophile.test.integration.common.components
  (:require
    [clojure.core.async :as async]
    [clojure.core.async.impl.protocols :as async.protocols]
    [clojure.string :as string]
    [com.ben-allred.audiophile.backend.api.repositories.core :as repos]
    [com.ben-allred.audiophile.backend.dev.migrations :as mig]
    [com.ben-allred.audiophile.backend.domain.interactors.core :as int]
    [com.ben-allred.audiophile.backend.infrastructure.db.core :as db]
    [com.ben-allred.audiophile.backend.api.pubsub.core :as ps]
    [com.ben-allred.audiophile.backend.api.pubsub.protocols :as pps]
    [com.ben-allred.audiophile.backend.infrastructure.pubsub.ws :as ws]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.common.core.utils.uuids :as uuids]
    [com.ben-allred.audiophile.common.infrastructure.http.core :as http]
    [hikari-cp.core :as hikari]
    [integrant.core :as ig]
    [next.jdbc :as jdbc]
    [next.jdbc.protocols :as pjdbc])
  (:import
    (java.io Closeable)
    (javax.sql DataSource)
    (org.projectodd.wunderboss.web.async Channel)))

(defmethod ig/init-key :audiophile.test/ws-handler [_ {:keys [pubsub]}]
  (fn [request]
    (let [msgs (async/chan 100)
          ctx (ws/build-ctx request pubsub 100)
          ch (reify
               pps/IChannel
               (open? [_]
                 (not (async.protocols/closed? msgs)))
               (send! [_ msg]
                 (async/>!! msgs msg))
               (close! [this]
                 (async/close! msgs)
                 (ws/on-close! ctx this)))]
      (ws/on-open! ctx ch)
      [::http/ok (reify
                   Channel

                   async.protocols/ReadPort
                   (take! [_ fn1-handler]
                     (async.protocols/take! msgs fn1-handler))

                   async.protocols/WritePort
                   (put! [_ val _]
                     (if (ps/open? ch)
                       (do (ws/on-message! ctx ch val)
                           (delay true))
                       (delay false)))

                   async.protocols/Channel
                   (closed? [_]
                     (not (ps/open? ch)))
                   (close! [_]
                     (ps/close! ch)))])))

(defmethod ig/init-key :audiophile.test/transactor [_ {:keys [->executor datasource migrator opts seed-data]}]
  (mig/migrate! migrator)
  (doto (db/->Transactor datasource opts ->executor)
    (repos/transact! (fn [executor]
                       (doseq [query seed-data]
                         (repos/execute! executor query))))))

(defmethod ig/init-key :audiophile.test/datasource [_ {:keys [spec]}]
  (let [datasource (hikari/make-datasource spec)
        db-name (str "audiophile_test_" (string/replace (str (uuids/random)) #"-" ""))]
    (jdbc/execute! datasource [(str "CREATE DATABASE " db-name)])
    (let [ds (hikari/make-datasource (assoc spec :database-name db-name))]
      (reify
        Closeable
        (close [_]
          (hikari/close-datasource ds)
          (jdbc/execute! datasource [(str "DROP DATABASE " db-name)])
          (hikari/close-datasource datasource))

        DataSource
        (getConnection [_]
          (.getConnection ds))

        pjdbc/Transactable
        (-transact [_ body-fn opts]
          (pjdbc/-transact ds body-fn opts))))))

(defmethod ig/halt-key! :audiophile.test/datasource [_ datasource]
  (.close datasource))

(defmethod ig/init-key :audiophile.test/rabbitmq#conn [_ _]
  (let [chs (atom nil)]
    (reify
      pps/IMQConnection
      (chan [_ {:keys [name]}]
        (swap! chs (fn [m]
                     (let [[ch tap] (or (get m name)
                                        (let [ch (async/chan 100)]
                                          [ch (async/mult ch)]))]
                       (assoc m name [ch tap]))))
        (reify
          pps/IMQChannel
          (subscribe! [_ handler _]
            (let [mq-ch (async/tap (second (get @chs name))
                                   (async/chan))]
              (async/go-loop []
                (when-let [msg (async/<! mq-ch)]
                  (when (int/handle? handler msg)
                    (int/handle! handler msg))
                  (recur)))))

          pps/IChannel
          (open? [_]
            (let [ch (first (get @chs name))]
              (not (async.protocols/closed? ch))))
          (send! [_ msg]
            (let [ch (first (get @chs name))]
              (async/go
                (async/>! ch msg))))
          (close! [_])))

      Closeable
      (close [_]
        (run! async/close! (map first (vals @chs)))))))

(defmethod ig/halt-key! :audiophile.test/rabbitmq#conn [_ conn]
  (.close ^Closeable conn))
