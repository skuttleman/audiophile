(ns audiophile.ui.system.components
  (:require
    [audiophile.ui.http.client :as client]
    [audiophile.ui.services.pubsub :as pubsub.ui]
    [audiophile.ui.store.impl :as istore]
    [audiophile.ui.utils.env :as env]
    [integrant.core :as ig]))

(defmethod ig/init-key :audiophile.ui.services/base-urls [_ cfg]
  (merge (select-keys env/env #{:api-base :auth-base})
         cfg))

(defmethod ig/init-key :audiophile.ui.services/http [_ cfg]
  (client/client cfg))

(defmethod ig/init-key :audiophile.ui.services/store [_ cfg]
  (istore/create cfg))

(defmethod ig/init-key :audiophile.ui.services/pubsub [_ cfg]
  (pubsub.ui/ws cfg))
