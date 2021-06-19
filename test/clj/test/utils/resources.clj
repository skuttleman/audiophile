(ns test.utils.resources
  (:require
    [clojure.java.io :as io]
    [com.ben-allred.audiophile.common.core.serdes.core :as serdes]
    [com.ben-allred.audiophile.common.infrastructure.duct :as uduct]))

(defn edn [resource]
  (serdes/deserialize (serdes/edn {})
                      (io/resource resource)
                      {:readers uduct/readers}))
