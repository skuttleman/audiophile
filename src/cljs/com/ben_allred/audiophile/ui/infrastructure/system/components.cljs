(ns com.ben-allred.audiophile.ui.infrastructure.system.components
  (:require
    [com.ben-allred.audiophile.ui.infrastructure.env :as env]
    [com.ben-allred.audiophile.ui.infrastructure.pages.login :as login]
    [com.ben-allred.audiophile.ui.infrastructure.services.ws :as ws]
    [com.ben-allred.audiophile.ui.infrastructure.store.impl :as istore]
    [com.ben-allred.audiophile.ui.infrastructure.http.client :as client]
    [integrant.core :as ig]))

(defmethod ig/init-key :audiophile.ui.services/base-urls [_ cfg]
  (merge (select-keys env/env #{:api-base :auth-base})
         cfg))

(defmethod ig/init-key :audiophile.ui.views/login-form [_ cfg]
  (login/form cfg))

(defmethod ig/init-key :audiophile.ui.services/http [_ cfg]
  (client/client cfg))

(defmethod ig/init-key :audiophile.ui.services/store [_ cfg]
  (istore/create cfg))

(defmethod ig/init-key :audiophile.ui.services/ws [_ cfg]
  (ws/client cfg))

(defmethod ig/halt-key! :audiophile.ui.services/ws [_ ws]
  (ws/client#close ws))
