(ns com.ben-allred.audiophile.ui.app
  (:require
    [com.ben-allred.audiophile.common.core.stubs.dom :as dom]
    [com.ben-allred.audiophile.common.infrastructure.ui-store.core :as ui-store]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.ui.infrastructure.system :as cfg]
    [integrant.core :as ig]
    [reagent.dom :as rdom]
    com.ben-allred.audiophile.common.infrastructure.system.core))

(defn ^:private app [app* store]
  [app* (ui-store/get-state store)])

(def ^:private config
  (cfg/load-config "ui.edn" [:duct.profile/base :duct.profile/prod]))

(defn init
  "runs when the browser page has loaded"
  ([]
   (set! log/*ctx* {:disabled? true})
   (init (ig/init config)))
  ([system]
   (let [{store :audiophile.services/ui-store
          app*  :audiophile.views/app} system]
     (rdom/render
       [app app* store]
       (.getElementById dom/document "root")
       #(log/info [:app/initialized])))))
