(ns audiophile.ui.views.layout.services
  (:require
    [audiophile.common.infrastructure.navigation.core :as nav]))

(defn nav#logout! [{:keys [nav]}]
  (fn [_]
    (nav/goto! nav :auth/logout)))

(defn nav#home [{:keys [nav]}]
  (nav/path-for nav :ui/home))
