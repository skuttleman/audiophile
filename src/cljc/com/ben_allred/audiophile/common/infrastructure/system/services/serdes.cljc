(ns com.ben-allred.audiophile.common.infrastructure.system.services.serdes
  (:require
    [com.ben-allred.audiophile.common.core.serdes.impl :as serde]
    [integrant.core :as ig]))

(defmethod ig/init-key :audiophile.services.serdes/edn [_ cfg]
  (serde/edn cfg))

(defmethod ig/init-key :audiophile.services.serdes/transit [_ cfg]
  (serde/transit cfg))

(defmethod ig/init-key :audiophile.services.serdes/json [_ cfg]
  (serde/json cfg))

(defmethod ig/init-key :audiophile.services.serdes/urlencode [_ cfg]
  (serde/urlencode cfg))

(defmethod ig/init-key :audiophile.services.serdes/jwt [_ cfg]
  (serde/jwt cfg))

(defmethod ig/init-key :audiophile.services.serdes/base64 [_ cfg]
  (serde/base64 cfg))
