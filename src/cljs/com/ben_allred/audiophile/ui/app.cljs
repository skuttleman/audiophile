(ns com.ben-allred.audiophile.ui.app
  (:require
    [clojure.pprint :as pp]
    [com.ben-allred.audiophile.common.api.navigation.core :as nav]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.common.infrastructure.http.core :as http]
    [com.ben-allred.audiophile.common.infrastructure.http.impl :as ihttp]
    [com.ben-allred.audiophile.ui.infrastructure.system.core :as sys]
    [com.ben-allred.audiophile.ui.infrastructure.pages.main :as main]
    [com.ben-allred.audiophile.ui.infrastructure.pages.login :as login]
    [com.ben-allred.vow.core :as v]
    [integrant.core :as ig]
    [reagent.dom :as rdom]
    react))

(def ^:private config
  (sys/load-config "ui.edn" [:duct.profile/base :duct.profile/prod]))

(defn ^:private render [comp & args]
  (v/create (fn [resolve _]
              (rdom/render (into [comp] args)
                           (.getElementById js/document "root")
                           resolve))))

(defn find-component [system k]
  (some-> system (ig/find-derived-1 k) val))

(defn init
  "runs when the browser page has loaded"
  ([]
   (set! log/*ctx* {:disabled? true})
   (init (ig/init config)))
  ([system]
   (if-let [nav (find-component system :services/nav)]
     (let [sys (find-component system :system.ui/components)]
       (pp/pprint system)
       (v/catch (v/and (-> ihttp/client
                           (http/get (nav/path-for nav :api/profile))
                           (v/then (fn [{profile :data}]
                                     (assert profile)
                                     (render main/root sys profile))
                                   (fn [_]
                                     (render login/root sys))))
                       (log/info [:app/initialized]))
                #(log/error "FAILED TO LOAD APP:" %)))
     (throw (ex-info "bad system (missing nav)" {:system system})))))
