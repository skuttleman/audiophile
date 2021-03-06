(ns audiophile.common.infrastructure.system.services.core
  (:require
    [audiophile.common.infrastructure.http.impl :as ihttp]
    [audiophile.common.infrastructure.navigation.base :as bnav]
    [audiophile.common.infrastructure.pubsub.memory :as pubsub.mem]
    [integrant.core :as ig]
    audiophile.common.infrastructure.system.services.serdes))

(defmethod ig/init-key :audiophile.services.nav/router [_ cfg]
  (bnav/router cfg))

(defmethod ig/init-key :audiophile.services.nav/nav [_ cfg]
  (bnav/nav cfg))

(defmethod ig/halt-key! :audiophile.services.nav/nav [_ cfg]
  (bnav/nav#stop cfg))

(defmethod ig/init-key :audiophile.services.pubsub/memory [_ cfg]
  (pubsub.mem/pubsub cfg))

(defmethod ig/init-key :audiophile.services.http/client [_ cfg]
  (ihttp/client cfg))
