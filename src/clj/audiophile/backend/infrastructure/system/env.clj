(ns audiophile.backend.infrastructure.system.env
  (:require
    [clojure.java.io :as io]
    [audiophile.common.core.serdes.core :as serdes]
    [audiophile.common.core.serdes.impl :as serde]
    [audiophile.common.core.utils.core :as u]
    [audiophile.common.core.utils.logger :as log]))

(defn ^:private file->env [file]
  (u/silent!
    (some->> file
             io/file
             slurp
             (serdes/deserialize serde/edn))))

(defn load-env
  "Loads edn files and builds a map of environment variables. Silently skips files that don't exist."
  [files]
  (transduce (map file->env) merge {} files))
