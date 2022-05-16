(ns com.ben-allred.audiophile.ui.app
  (:require
    [clojure.pprint :as pp]
    [com.ben-allred.audiophile.common.api.navigation.core :as nav]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.common.infrastructure.http.core :as http]
    [com.ben-allred.audiophile.common.infrastructure.http.impl :as ihttp]
    [com.ben-allred.audiophile.ui.infrastructure.pages.login :as login]
    [com.ben-allred.audiophile.ui.infrastructure.pages.main :as main]
    [com.ben-allred.audiophile.ui.infrastructure.system.core :as sys]
    [com.ben-allred.vow.core :as v :include-macros true]
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

(defn ^:private load-profile! [{:keys [http-client] :as sys} nav]
  (-> (http/get http-client (nav/path-for nav :api/profile))
      (v/then (fn [{profile :data}]
                (assert profile)
                (v/and (render main/root sys profile)
                       (log/info [:app/initialized])))
              (fn [e]
                (v/and (render login/root sys)
                       (log/error [:app/failed e]))))))

(defn find-component [system k]
  (some-> system (ig/find-derived-1 k) val))

(defn init
  "runs when the browser page has loaded"
  ([]
   (set! log/*ctx* {:disabled? true})
   (init (ig/init config)))
  ([system]
   (if-let [nav (find-component system :services/nav)]
     (do (pp/pprint system)
         (-> system
             (find-component :system.ui/components)
             (assoc :http-client ihttp/client)
             (load-profile! nav)))
     (throw (ex-info "bad system" {:system system})))))
