(ns com.ben-allred.audiophile.integration.common.components
  (:require
    [clojure.core.async :as async]
    [clojure.core.async.impl.protocols :as async.protocols]
    [clojure.string :as string]
    [com.ben-allred.audiophile.api.dev.migrations :as mig]
    [com.ben-allred.audiophile.api.infrastructure.db.core :as db]
    [com.ben-allred.audiophile.api.infrastructure.pubsub.protocols :as pws]
    [com.ben-allred.audiophile.api.infrastructure.pubsub.ws :as ws]
    [com.ben-allred.audiophile.api.app.repositories.core :as repos]
    [com.ben-allred.audiophile.common.core.serdes.core :as serdes]
    [com.ben-allred.audiophile.common.core.resources.http :as http]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.common.core.utils.uuids :as uuids]
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
      (ws/on-open handler)
      [::http/ok (reify
                   Channel

                   async.protocols/ReadPort
                   (take! [_ fn1-handler]
                     (async.protocols/take! msgs fn1-handler))

                   async.protocols/WritePort
                   (put! [_ val _]
                     (if (ws/open? channel)
                       (do (ws/on-message handler (serdes/serialize serializer val))
                           (delay true))
                       (delay false)))

                   async.protocols/Channel
                   (closed? [_]
                     (not (ws/open? channel)))
                   (close! [_]
                     (when (ws/open? channel)
                       (ws/on-close handler))
                     (ws/close! channel)))])))

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
