(ns audiophile.ui.views.layout.services
  (:require
    [audiophile.common.infrastructure.navigation.core :as nav]))

(defn nav#home [{:keys [nav]}]
  (nav/path-for nav :routes.ui/home))
