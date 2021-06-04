(ns com.ben-allred.audiophile.common.infrastructure.system.core
  (:require
    [com.ben-allred.audiophile.common.infrastructure.ui-store.hooks :as store.hooks]
    [integrant.core :as ig]
    com.ben-allred.audiophile.common.infrastructure.system.services.core
    com.ben-allred.audiophile.common.infrastructure.system.views))

(defmethod ig/init-key :audiophile.core/banners [_ cfg]
  (store.hooks/banners cfg))

(defmethod ig/init-key :audiophile.core/toasts [_ cfg]
  (store.hooks/toasts cfg))

(defmethod ig/init-key :audiophile.core/modals [_ cfg]
  (store.hooks/modals cfg))

(defmethod ig/init-key :audiophile.core/route-link [_ cfg]
  (store.hooks/route-link cfg))
