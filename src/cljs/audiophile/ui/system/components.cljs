(ns audiophile.ui.system.components
  (:require
    [audiophile.ui.utils.env :as env]
    [audiophile.ui.store.impl :as istore]
    [audiophile.ui.http.client :as client]
    [integrant.core :as ig]))

(defmethod ig/init-key :audiophile.ui.services/base-urls [_ cfg]
  (merge (select-keys env/env #{:api-base :auth-base})
         cfg))

(defmethod ig/init-key :audiophile.ui.services/http [_ cfg]
  (client/client cfg))

(defmethod ig/init-key :audiophile.ui.services/store [_ cfg]
  (istore/create cfg))
