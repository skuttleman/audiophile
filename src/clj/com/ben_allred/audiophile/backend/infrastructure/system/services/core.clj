(ns com.ben-allred.audiophile.backend.infrastructure.system.services.core
  (:require
    [com.ben-allred.audiophile.backend.infrastructure.auth.core :as auth]
    [com.ben-allred.audiophile.backend.infrastructure.auth.google :as goog]
    [com.ben-allred.audiophile.backend.infrastructure.db.common :as cdb]
    [com.ben-allred.audiophile.backend.infrastructure.pubsub.rabbit :as pub.rabbit]
    [com.ben-allred.audiophile.backend.infrastructure.pubsub.ws :as ws]
    [com.ben-allred.audiophile.backend.infrastructure.pubsub.handlers.comments :as pub.comments]
    [com.ben-allred.audiophile.backend.infrastructure.pubsub.handlers.files :as pub.files]
    [com.ben-allred.audiophile.backend.infrastructure.pubsub.handlers.projects :as pub.projects]
    [com.ben-allred.audiophile.backend.infrastructure.pubsub.handlers.teams :as pub.teams]
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

(defmethod ig/init-key :audiophile.services.rabbitmq/command-handler#comments [_ cfg]
  (pub.comments/msg-handler cfg))

(defmethod ig/init-key :audiophile.services.rabbitmq/command-handler#files [_ cfg]
  (pub.files/msg-handler cfg))

(defmethod ig/init-key :audiophile.services.rabbitmq/command-handler#projects [_ cfg]
  (pub.projects/msg-handler cfg))

(defmethod ig/init-key :audiophile.services.rabbitmq/command-handler#teams [_ cfg]
  (pub.teams/msg-handler cfg))
