(ns com.ben-allred.audiophile.ui.app
  (:require
    [com.ben-allred.audiophile.common.services.ui-store.core :as ui-store]
    [com.ben-allred.audiophile.common.utils.dom :as dom]
    [com.ben-allred.audiophile.common.utils.logger :as log]
    [com.ben-allred.audiophile.common.views.core :as views]
    [com.ben-allred.audiophile.ui.services.config :as cfg]
    [integrant.core :as ig]
    [reagent.dom :as rdom]
    com.ben-allred.audiophile.common.services.http
    com.ben-allred.audiophile.common.services.navigation.core
    com.ben-allred.audiophile.common.services.navigation.parsers
    com.ben-allred.audiophile.common.services.resources.core
    com.ben-allred.audiophile.common.services.resources.users
    com.ben-allred.audiophile.common.views.components.toast
    com.ben-allred.audiophile.common.views.roots.home
    com.ben-allred.audiophile.common.views.roots.login
    [com.ben-allred.audiophile.common.services.navigation.core :as nav]
    [com.ben-allred.audiophile.common.utils.uuids :as uuids]))

(defn ^:private app [app* store]
  [app* (-> store
            ui-store/get-state
            (dissoc :internal/resource-state))])

(defonce ^:private sys
  (atom nil))

(defmethod ig/init-key :duct/const [_ component]
  component)

(defmethod ig/init-key :duct.core/project-ns [_ ns]
  ns)

(defn init []
  (let [{store ::ui-store/store
         app*  ::views/app} (swap! sys (fn [sys]
                                         (some-> sys ig/halt!)
                                         (ig/init (cfg/load-config "ui.edn"))))]
    (rdom/render
      [app app* store]
      (.getElementById dom/document "root")
      #(log/info [:app/initialized]))))
