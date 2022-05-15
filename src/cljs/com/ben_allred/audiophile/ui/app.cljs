(ns com.ben-allred.audiophile.ui.app
  (:require
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [reagent.dom :as rdom]))

(defn app []
  [:div "hello world"])

(defn init
  "runs when the browser page has loaded"
  ([]
   (set! log/*ctx* {:disabled? true})
   (init nil))
  ([system]
   (rdom/render
     [app]
     (.getElementById js/document "root")
     #(log/info [:app/initialized]))))
