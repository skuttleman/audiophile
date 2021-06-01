(ns com.ben-allred.audiophile.api.infrastructure.system.services.core
  (:require
    [com.ben-allred.audiophile.api.infrastructure.auth.core :as auth]
    [com.ben-allred.audiophile.api.infrastructure.auth.google :as goog]
    [com.ben-allred.audiophile.api.infrastructure.pubsub.ws :as ws]
    [com.ben-allred.audiophile.api.infrastructure.resources.s3 :as s3]
    [integrant.core :as ig]
    com.ben-allred.audiophile.api.infrastructure.system.services.repositories))

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
