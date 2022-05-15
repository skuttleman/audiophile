(ns com.ben-allred.audiophile.common.infrastructure.system.services.serdes
  (:require
    [com.ben-allred.audiophile.common.core.serdes.impl :as serde]
    [integrant.core :as ig]))

(defmethod ig/init-key :audiophile.services.serdes/jwt [_ cfg]
  (serde/jwt cfg))
