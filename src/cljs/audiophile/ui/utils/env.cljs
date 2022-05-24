(ns audiophile.ui.utils.env
  (:require
    [audiophile.common.core.serdes.core :as serdes]
    [audiophile.common.core.serdes.impl :as serde]))

(def env
  (serdes/deserialize serde/transit js/window.ENV))
