(ns com.ben-allred.audiophile.common.config.services.serdes
  (:require
    [com.ben-allred.audiophile.common.services.serdes.core :as serdes]
    [integrant.core :as ig]))

(defmethod ig/init-key :audiophile.services.serdes/edn [_ cfg]
  (serdes/edn cfg))

(defmethod ig/init-key :audiophile.services.serdes/transit [_ cfg]
  (serdes/transit cfg))

(defmethod ig/init-key :audiophile.services.serdes/json [_ cfg]
  (serdes/json cfg))

(defmethod ig/init-key :audiophile.services.serdes/urlencode [_ cfg]
  (serdes/urlencode cfg))

(defmethod ig/init-key :audiophile.services.serdes/jwt [_ cfg]
  (serdes/jwt cfg))
