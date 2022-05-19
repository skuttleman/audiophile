(ns com.ben-allred.audiophile.ui.infrastructure.system.components
  (:require
    [com.ben-allred.audiophile.ui.infrastructure.env :as env]
    [com.ben-allred.audiophile.ui.infrastructure.pages.login :as login]
    [com.ben-allred.audiophile.ui.infrastructure.store.impl :as istore]
    [integrant.core :as ig]))

(defmethod ig/init-key :audiophile.ui.services/base-urls [_ cfg]
  (merge (select-keys env/env #{:api-base :auth-base})
         cfg))

(defmethod ig/init-key :audiophile.ui.views/login-form [_ cfg]
  (login/form cfg))

(defmethod ig/init-key :audiophile.services.store/store [_ cfg]
  (istore/create cfg))
