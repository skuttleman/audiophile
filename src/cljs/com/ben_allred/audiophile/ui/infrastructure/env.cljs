(ns com.ben-allred.audiophile.ui.infrastructure.env
  (:require
    [com.ben-allred.audiophile.common.core.serdes.core :as serdes]
    [com.ben-allred.audiophile.common.core.serdes.impl :as serde]))

(def env
  (serdes/deserialize serde/transit js/window.ENV))
