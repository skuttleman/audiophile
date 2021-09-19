(ns com.ben-allred.audiophile.common.infrastructure.system.services.http
  (:require
    [com.ben-allred.audiophile.common.infrastructure.http.impl :as client]
    [integrant.core :as ig]))

(defmethod ig/init-key :audiophile.services.http/base [_ cfg]
  (client/base cfg))

(defmethod ig/init-key :audiophile.services.http/with-logging [_ cfg]
  (client/with-logging cfg))

(defmethod ig/init-key :audiophile.services.http/with-headers [_ cfg]
  (client/with-headers cfg))

(defmethod ig/init-key :audiophile.services.http/with-serde [_ cfg]
  (client/with-serde cfg))

(defmethod ig/init-key :audiophile.services.http/with-nav [_ cfg]
  (client/with-nav cfg))

(defmethod ig/init-key :audiophile.services.http/with-pubsub [_ cfg]
  (client/with-pubsub cfg))

(defmethod ig/init-key :audiophile.services.http/client [_ cfg]
  (client/client cfg))
