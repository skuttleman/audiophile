(ns com.ben-allred.audiophile.ui.infrastructure.system.core
  (:require
    [com.ben-allred.audiophile.ui.infrastructure.pubsub.ws :as ws]
    [com.ben-allred.audiophile.ui.infrastructure.store.core :as store]
    [com.ben-allred.audiophile.ui.infrastructure.store.hooks :as store.hooks]
    [integrant.core :as ig]
    com.ben-allred.audiophile.common.infrastructure.system.services.core
    com.ben-allred.audiophile.ui.infrastructure.system.services.resources
    com.ben-allred.audiophile.ui.infrastructure.system.views))

(defmethod ig/init-key :audiophile.core/banners [_ cfg]
  (store.hooks/banners cfg))

(defmethod ig/init-key :audiophile.core/toasts [_ cfg]
  (store.hooks/toasts cfg))

(defmethod ig/init-key :audiophile.core/modals [_ cfg]
  (store.hooks/modals cfg))

(defmethod ig/init-key :audiophile.core/route-link [_ cfg]
  (store.hooks/route-link cfg))

(defmethod ig/init-key :audiophile.services.nav/tracker [_ cfg]
  (store.hooks/tracker cfg))

(defmethod ig/init-key :audiophile.services/store [_ cfg]
  (store/store cfg))

(defmethod ig/init-key :audiophile.services.ws/client [_ cfg]
  (ws/client cfg))

(defmethod ig/halt-key! :audiophile.services.ws/client [_ ws-handler]
  (ws/client#close ws-handler))
