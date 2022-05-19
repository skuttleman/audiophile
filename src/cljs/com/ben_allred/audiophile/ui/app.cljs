(ns com.ben-allred.audiophile.ui.app
  (:require
    [com.ben-allred.audiophile.common.api.navigation.core :as nav]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.common.infrastructure.http.core :as http]
    [com.ben-allred.audiophile.ui.infrastructure.pages.login :as login]
    [com.ben-allred.audiophile.ui.infrastructure.pages.main :as main]
    [com.ben-allred.audiophile.ui.infrastructure.system.core :as sys]
    [com.ben-allred.vow.core :as v :include-macros true]
    [integrant.core :as ig]
    [reagent.dom :as rdom]))

(def ^:private config
  (sys/load-config "ui.edn" [:duct.profile/base :duct.profile/prod]))

(defn ^:private render [comp & args]
  (v/create (fn [resolve _]
              (rdom/render (into [comp] args)
                           (.getElementById js/document "root")
                           resolve))))

(defn ^:private load-profile! [{:keys [http-client nav] :as sys}]
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
   (init (ig/init config [:system.ui/components])))
  ([system]
   (if-let [sys (find-component system :system.ui/components)]
     (load-profile! sys)
     (throw (ex-info "bad system" {:system system})))))
