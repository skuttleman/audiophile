(ns com.ben-allred.audiophile.common.infrastructure.system.services.core
  (:require
    [com.ben-allred.audiophile.common.api.navigation.base :as bnav]
    [com.ben-allred.audiophile.common.infrastructure.http.impl :as ihttp]
    [com.ben-allred.audiophile.common.infrastructure.pubsub.memory :as pubsub.mem]
    [integrant.core :as ig]
    com.ben-allred.audiophile.common.infrastructure.system.services.serdes))

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
