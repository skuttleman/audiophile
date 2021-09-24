(ns com.ben-allred.audiophile.backend.infrastructure.system.services.core
  (:require
    [com.ben-allred.audiophile.backend.infrastructure.auth.core :as auth]
    [com.ben-allred.audiophile.backend.infrastructure.auth.google :as goog]
    [com.ben-allred.audiophile.backend.infrastructure.db.common :as cdb]
    [com.ben-allred.audiophile.backend.infrastructure.pubsub.rabbit :as pubsub.rabbit]
    [com.ben-allred.audiophile.backend.infrastructure.pubsub.ws :as ws]
    [com.ben-allred.audiophile.backend.infrastructure.resources.s3 :as s3]
    [integrant.core :as ig]
    com.ben-allred.audiophile.backend.infrastructure.system.services.repositories))

(defmethod ig/init-key :audiophile.services.auth/provider [_ cfg]
  (auth/interactor cfg))

(defmethod ig/init-key :audiophile.services.auth.google/provider [_ cfg]
  (goog/provider cfg))

(defmethod ig/init-key :audiophile.services.ws/->channel [_ cfg]
  (ws/->channel cfg))

(defmethod ig/init-key :audiophile.services.ws/->handler [_ cfg]
  (ws/->handler cfg))

(defmethod ig/init-key :audiophile.services.ws/handler [_ cfg]
  (ws/handler cfg))

(defmethod ig/init-key :audiophile.services.s3/client [_ cfg]
  (s3/client cfg))

(defmethod ig/init-key :audiophile.services.s3/stream-serde [_ cfg]
  (s3/stream-serde cfg))

(defmethod ig/init-key :audiophile.services.rabbitmq/conn [_ cfg]
  (pubsub.rabbit/conn cfg))

(defmethod ig/halt-key! :audiophile.services.rabbitmq/conn [_ cfg]
  (pubsub.rabbit/conn#stop cfg))

(defmethod ig/init-key :audiophile.services.rabbitmq/publisher [_ rabbitmq]
  (pubsub.rabbit/publisher rabbitmq))

(defmethod ig/init-key :audiophile.services.rabbitmq/subscriber [_ rabbitmq]
  (pubsub.rabbit/subscriber rabbitmq))

(defmethod ig/init-key :audiophile.services.rabbitmq/db-handler [_ cfg]
  (cdb/event->db-handler cfg))

(defmethod ig/init-key :audiophile.services.rabbitmq/ws-handler [_ cfg]
  (ws/event->ws-handler cfg))
