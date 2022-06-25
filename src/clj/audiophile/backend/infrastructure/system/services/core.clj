(ns audiophile.backend.infrastructure.system.services.core
  (:require
    [audiophile.backend.infrastructure.auth.core :as auth]
    [audiophile.backend.infrastructure.auth.google :as goog]
    [audiophile.backend.infrastructure.db.common :as cdb]
    [audiophile.backend.infrastructure.pubsub.handlers.workflows :as pub.workflows]
    [audiophile.backend.infrastructure.pubsub.rabbit :as pub.rabbit]
    [audiophile.backend.infrastructure.pubsub.ws :as ws]
    [audiophile.backend.infrastructure.resources.s3 :as s3]
    [integrant.core :as ig]
    audiophile.backend.infrastructure.pubsub.handlers.comments
    audiophile.backend.infrastructure.pubsub.handlers.files
    audiophile.backend.infrastructure.pubsub.handlers.projects
    audiophile.backend.infrastructure.pubsub.handlers.teams
    audiophile.backend.infrastructure.pubsub.handlers.users
    audiophile.backend.infrastructure.system.services.repositories))

(defmethod ig/init-key :audiophile.services.auth/provider [_ cfg]
  (auth/interactor cfg))

(defmethod ig/init-key :audiophile.services.auth.google/provider [_ cfg]
  (goog/provider cfg))

(defmethod ig/init-key :audiophile.services.ws/handler [_ cfg]
  (ws/handler cfg))

(defmethod ig/init-key :audiophile.services.s3/client [_ cfg]
  (s3/client cfg))

(defmethod ig/init-key :audiophile.services.s3/stream-serde [_ cfg]
  (s3/stream-serde cfg))

(defmethod ig/init-key :audiophile.services.rabbitmq/conn [_ cfg]
  (pub.rabbit/conn cfg))

(defmethod ig/halt-key! :audiophile.services.rabbitmq/conn [_ cfg]
  (pub.rabbit/conn#stop cfg))

(defmethod ig/init-key :audiophile.services.rabbitmq/channel [_ cfg]
  (pub.rabbit/channel cfg))

(defmethod ig/init-key :audiophile.services.rabbitmq/subscriber [_ cfg]
  (pub.rabbit/subscriber cfg))

(defmethod ig/init-key :audiophile.rabbitmq.exchange/name [_ cfg]
  (pub.rabbit/exchange cfg))

(defmethod ig/init-key :audiophile.services.rabbitmq/db-handler [_ cfg]
  (cdb/event->db-handler cfg))

(defmethod ig/init-key :audiophile.services.rabbitmq/ws-handler [_ cfg]
  (ws/event->ws-handler cfg))

(defmethod ig/init-key :audiophile.services.rabbitmq/command-handler#workflows [_ cfg]
  (pub.workflows/msg-handler cfg))
