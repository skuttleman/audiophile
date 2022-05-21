(ns audiophile.common.infrastructure.system.services.serdes
  (:require
    [audiophile.common.core.serdes.impl :as serde]
    [integrant.core :as ig]))

(defmethod ig/init-key :audiophile.services.serdes/jwt [_ cfg]
  (serde/jwt cfg))
