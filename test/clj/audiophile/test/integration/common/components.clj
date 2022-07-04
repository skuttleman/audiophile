(ns audiophile.test.integration.common.components
  (:require
    [audiophile.backend.api.pubsub.core :as ps]
    [audiophile.backend.api.pubsub.protocols :as pps]
    [audiophile.backend.dev.migrations :as mig]
    [audiophile.backend.domain.interactors.core :as int]
    [audiophile.backend.infrastructure.db.core :as db]
    [audiophile.backend.infrastructure.db.models.sql :as sql]
    [audiophile.backend.infrastructure.pubsub.ws :as ws]
    [audiophile.common.core.utils.logger :as log]
    [audiophile.common.core.utils.uuids :as uuids]
    [audiophile.common.infrastructure.http.core :as http]
    [clojure.core.async :as async]
    [clojure.core.async.impl.protocols :as async.protocols]
    [clojure.string :as string]
    [hikari-cp.core :as hikari]
    [integrant.core :as ig]
    [next.jdbc :as jdbc]
    [next.jdbc.protocols :as pjdbc]
    [spigot.controllers.kafka.common :as sp.kcom]
    [spigot.controllers.kafka.core :as sp.kafka])
  (:import
    (java.io Closeable)
    (java.sql Timestamp)
    (javax.sql DataSource)
    (org.projectodd.wunderboss.web.async Channel)
    (org.apache.kafka.streams TestInputTopic TestOutputTopic TopologyTestDriver)))

(defn migrate! [datasource]
  (mig/migrate! (mig/create-migrator datasource)))

(defn seed! [datasource seed-data]
  (doseq [query seed-data]
    (jdbc/execute! datasource
                   (mapv (fn [x]
                           (cond-> x
                             (inst? x) (-> .getTime Timestamp.)))
                         (sql/format query)))))

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

(defmethod ig/init-key :audiophile.test/transactor [_ {:keys [->executor datasource opts]}]
  (db/->Transactor datasource opts ->executor))

(defmethod ig/init-key :audiophile.test/datasource [_ {:keys [seed-data spec]}]
  (let [datasource (hikari/make-datasource spec)
        db-name (str "audiophile_test_" (string/replace (str (uuids/random)) #"-" ""))]
    (jdbc/execute! datasource [(str "CREATE DATABASE " db-name)])
    (let [ds (hikari/make-datasource (assoc spec :database-name db-name))]
      (migrate! ds)
      (seed! ds seed-data)
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

(defmethod ig/init-key :audiophile.test/kafka#test-driver
  [_ {:keys [event-topic-cfg workflow-topic-cfg] :as opts}]
  (let [driver (-> (sp.kafka/default-builder)
                   (sp.kafka/with-wf-topology opts)
                   (sp.kafka/with-task-topology opts)
                   .build
                   (TopologyTestDriver. (sp.kcom/->props {:application.id    (str (uuids/random))
                                                          :bootstrap.servers "fake"})))]
    {:driver driver
     :events (.createOutputTopic driver
                                 (:name event-topic-cfg)
                                 (.deserializer (:key-serde workflow-topic-cfg))
                                 (.deserializer (:val-serde workflow-topic-cfg)))
     :workflows (.createInputTopic driver
                                   (:name workflow-topic-cfg)
                                   (.serializer (:key-serde workflow-topic-cfg))
                                   (.serializer (:val-serde workflow-topic-cfg)))}))

(defmethod ig/halt-key! :audiophile.test/kafka#test-driver
  [_ {:keys [^Closeable driver]}]
  (.close driver))

(defmethod ig/init-key :audiophile.test/kafka#consumer
  [_ {{:keys [^TestOutputTopic events]} :test-driver :keys [listeners]}]
  (let [poll? (volatile! true)]
    (async/go-loop []
      (doseq [event (.readValuesToList events)
              listener listeners]
        (listener {:value event}))
      (async/<! (async/timeout 100))
      (when @poll?
        (recur)))
    poll?))

(defmethod ig/halt-key! :audiophile.test/kafka#consumer [_ poll?]
  (vreset! poll? false))

(defmethod ig/init-key :audiophile.test/kafka#producer
  [_ {{:keys [^TestInputTopic workflows]} :test-driver}]
  (reify pps/IChannel
    (send! [_ {:keys [key value]}]
      (.pipeInput workflows key value))))
