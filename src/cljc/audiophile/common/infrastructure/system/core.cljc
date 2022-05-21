(ns audiophile.common.infrastructure.system.core
  (:require
    [integrant.core :as ig]
    audiophile.common.infrastructure.system.services.core
    audiophile.common.infrastructure.system.utils))

(defmethod ig/init-key :duct.custom/merge [_ ms]
  (reduce merge ms))
