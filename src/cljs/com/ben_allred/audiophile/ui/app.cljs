(ns com.ben-allred.audiophile.ui.app
  (:require
    [com.ben-allred.audiophile.common.utils.dom :as dom]
    [com.ben-allred.audiophile.common.views.core :as views]
    [reagent.dom :as rdom]))

(defn init []
  (rdom/render
    [views/app {:text "foo"}]
    (.getElementById dom/document "root")
    #(println [:app/initialized])))

