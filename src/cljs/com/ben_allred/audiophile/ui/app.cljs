(ns com.ben-allred.audiophile.ui.app
  (:require
    [com.ben-allred.audiophile.common.services.ui-store.core :as ui-store]
    [com.ben-allred.audiophile.common.services.stubs.dom :as dom]
    [com.ben-allred.audiophile.common.utils.logger :as log]
    [com.ben-allred.audiophile.common.views.core :as views]
    [com.ben-allred.audiophile.ui.services.config :as cfg]
    [integrant.core :as ig]
    [reagent.dom :as rdom]
    com.ben-allred.audiophile.common.services.http
    com.ben-allred.audiophile.common.services.navigation.core
    com.ben-allred.audiophile.common.services.navigation.parsers
    com.ben-allred.audiophile.common.services.resources.core
    com.ben-allred.audiophile.common.services.resources.toaster
    com.ben-allred.audiophile.common.services.resources.users
    com.ben-allred.audiophile.common.views.components.toast
    com.ben-allred.audiophile.common.views.roots.home
    com.ben-allred.audiophile.common.views.roots.login))

(defn ^:private app [app* store]
  [app* (-> store
            ui-store/get-state
            (dissoc :internal/resource-state))])

(defmethod ig/init-key :duct/const [_ component]
  component)

(defmethod ig/init-key :duct.core/project-ns [_ ns]
  ns)

(defmethod ig/init-key :duct.core/environment [_ env]
  env)

(def ^:private config
  (cfg/load-config "ui.edn" [:duct.profile/base :duct.profile/prod]))

(defn ^:export init
  ([]
   (init (ig/init config)))
  ([system]
   (let [{store ::ui-store/store
          app*  ::views/app} system]
     (rdom/render
       [app app* store]
       (.getElementById dom/document "root")
       #(log/info [:app/initialized])))))
