(ns com.ben-allred.audiophile.common.infrastructure.system.services.core
  (:require
    [com.ben-allred.audiophile.common.app.navigation.base :as bnav]
    [com.ben-allred.audiophile.common.infrastructure.pubsub.core :as pubsub]
    [com.ben-allred.audiophile.common.infrastructure.pubsub.ws :as ws]
    [com.ben-allred.audiophile.common.infrastructure.ui-store.core :as ui-store]
    [com.ben-allred.audiophile.common.infrastructure.ui-store.hooks :as store.hooks]
    [integrant.core :as ig]
    com.ben-allred.audiophile.common.infrastructure.system.services.http
    com.ben-allred.audiophile.common.infrastructure.system.services.resources
    com.ben-allred.audiophile.common.infrastructure.system.services.serdes))

(defmethod ig/init-key :audiophile.services/ui-store [_ cfg]
  (ui-store/store cfg))

(defmethod ig/init-key :audiophile.services.nav/tracker [_ cfg]
  (store.hooks/tracker cfg))

(defmethod ig/init-key :audiophile.services.nav/router [_ cfg]
  (bnav/router cfg))

(defmethod ig/init-key :audiophile.services.nav/nav [_ cfg]
  (bnav/nav cfg))

(defmethod ig/halt-key! :audiophile.services.nav/nav [_ cfg]
  (bnav/nav#stop cfg))

(defmethod ig/init-key :audiophile.services/pubsub [_ cfg]
  (pubsub/pubsub cfg))

(defmethod ig/init-key :audiophile.services.ws/client [_ cfg]
  (ws/client cfg))

(defmethod ig/halt-key! :audiophile.services.ws/client [_ ws-handler]
  (ws/client#close ws-handler))
