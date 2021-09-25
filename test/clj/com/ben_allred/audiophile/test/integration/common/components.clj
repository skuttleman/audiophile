(ns com.ben-allred.audiophile.test.integration.common.components
  (:require
    [clojure.core.async :as async]
    [clojure.core.async.impl.protocols :as async.protocols]
    [clojure.string :as string]
    [com.ben-allred.audiophile.backend.api.repositories.core :as repos]
    [com.ben-allred.audiophile.backend.dev.migrations :as mig]
    [com.ben-allred.audiophile.backend.domain.interactors.core :as int]
    [com.ben-allred.audiophile.backend.infrastructure.db.core :as db]
    [com.ben-allred.audiophile.backend.infrastructure.pubsub.core :as ps]
    [com.ben-allred.audiophile.backend.infrastructure.pubsub.protocols :as pws]
    [com.ben-allred.audiophile.common.core.serdes.core :as serdes]
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

(defmethod ig/init-key :audiophile.test/ws-handler [_ {:keys [->channel ->handler serdes]}]
  (fn [request]
    (let [params (get-in request [:nav/route :query-params])
          serializer (serdes/find-serde! serdes
                                         (or (:content-type params)
                                             (:accept params)
                                             ""))
          deserializer (serdes/find-serde! serdes
                                           (or (:accept params)
                                               (:content-type params)
                                               ""))
          msgs (async/chan 100)
          channel (->channel request
                             (reify pws/IChannel
                               (open? [_]
                                 (not (async.protocols/closed? msgs)))
                               (send! [_ msg]
                                 (async/>!! msgs (serdes/deserialize deserializer msg)))
                               (close! [_]
                                 (async/close! msgs))))
          handler (->handler request channel)]
      (ps/on-open handler)
      [::http/ok (reify
                   Channel

                   async.protocols/ReadPort
                   (take! [_ fn1-handler]
                     (async.protocols/take! msgs fn1-handler))

                   async.protocols/WritePort
                   (put! [_ val _]
                     (if (ps/open? channel)
                       (do (ps/on-message handler (serdes/serialize serializer val))
                           (delay true))
                       (delay false)))

                   async.protocols/Channel
                   (closed? [_]
                     (not (ps/open? channel)))
                   (close! [_]
                     (when (ps/open? channel)
                       (ps/on-close handler))
                     (ps/close! channel)))])))

(defmethod ig/init-key :audiophile.test/transactor [_ {:keys [->executor datasource migrator opts seed-data]}]
  (mig/migrate! migrator)
  (doto (db/->Transactor datasource opts ->executor)
    (repos/transact! (fn [executor]
                       (doseq [query seed-data]
                         (repos/execute! executor query))))))

(defmethod ig/init-key :audiophile.test/datasource [_ {:keys [spec]}]
  (let [datasource (hikari/make-datasource spec)
        db-name (str "test_db_" (string/replace (str (uuids/random)) #"-" ""))]
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
      pws/IMQConnection
      (chan [_ {:keys [name]}]
        (swap! chs (fn [m]
                     (let [[ch tap] (or (get m name)
                                        (let [ch (async/chan 100)]
                                          [ch (async/mult ch)]))]
                       (assoc m name [ch tap]))))
        (reify
          pws/IMQChannel
          (subscribe! [_ handler _]
            (let [mq-ch (async/tap (second (get @chs name))
                                   (async/chan))]
              (async/go-loop []
                (when-let [msg (async/<! mq-ch)]
                  (int/handle! handler msg)
                  (recur)))))

          pws/IChannel
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