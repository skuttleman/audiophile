(ns com.ben-allred.audiophile.common.config.services.core
  (:require
    [com.ben-allred.audiophile.common.services.navigation.core :as nav]
    [com.ben-allred.audiophile.common.services.pubsub.core :as pubsub]
    [com.ben-allred.audiophile.common.services.pubsub.ws :as ws]
    [com.ben-allred.audiophile.common.services.ui-store.core :as ui-store]
    [integrant.core :as ig]
    com.ben-allred.audiophile.common.config.services.http
    com.ben-allred.audiophile.common.config.services.resources
    com.ben-allred.audiophile.common.config.services.serdes))

(defmethod ig/init-key :audiophile.services/ui-store [_ cfg]
  (ui-store/store cfg))

(defmethod ig/init-key :audiophile.services.nav/router [_ cfg]
  (nav/router cfg))

(defmethod ig/init-key :audiophile.services.nav/nav [_ cfg]
  (nav/nav cfg))

(defmethod ig/halt-key! :audiophile.services.nav/nav [_ cfg]
  (nav/nav#stop cfg))

(defmethod ig/init-key :audiophile.services/pubsub [_ cfg]
  (pubsub/pubsub cfg))

(defmethod ig/init-key :audiophile.services.ws/handler [_ cfg]
  (ws/handler cfg))

(defmethod ig/halt-key! :audiophile.services.ws/handler [_ ws-handler]
  (ws/handler#close ws-handler))
