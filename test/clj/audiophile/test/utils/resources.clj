(ns audiophile.test.utils.resources
  (:require
    [clojure.java.io :as io]
    [audiophile.common.core.serdes.core :as serdes]
    [audiophile.common.core.serdes.impl :as serde]
    [audiophile.common.infrastructure.duct :as uduct]))

(defn edn [resource]
  (serdes/deserialize serde/edn
                      (io/resource resource)
                      {:readers uduct/readers}))
