(ns com.ben-allred.audiophile.common.infrastructure.system.core
  (:require
    [integrant.core :as ig]
    com.ben-allred.audiophile.common.infrastructure.system.services.core
    com.ben-allred.audiophile.common.infrastructure.system.utils))

(defmethod ig/init-key :duct.custom/merge [_ ms]
  (reduce merge ms))
