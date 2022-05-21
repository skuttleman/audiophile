(ns com.ben-allred.audiophile.ui.app
  (:require
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.common.infrastructure.store.core :as store]
    [com.ben-allred.audiophile.ui.infrastructure.pages.login :as login]
    [com.ben-allred.audiophile.ui.infrastructure.pages.main :as main]
    [com.ben-allred.audiophile.ui.infrastructure.services.ws :as ws]
    [com.ben-allred.audiophile.ui.infrastructure.store.actions :as act]
    [com.ben-allred.audiophile.ui.infrastructure.system.core :as sys]
    [com.ben-allred.vow.core :as v :include-macros true]
    [integrant.core :as ig]
    [reagent.dom :as rdom]
    com.ben-allred.audiophile.ui.infrastructure.store.async
    com.ben-allred.audiophile.ui.infrastructure.store.mutations))

(def ^:private config
  (sys/load-config "ui.edn" [:duct.profile/base :duct.profile/prod]))

(defn ^:private render [comp]
  (v/create (fn [resolve _]
              (rdom/render comp
                           (.getElementById js/document "root")
                           resolve))))

(defn ^:private init* [{:keys [store] :as sys}]
  (store/init! store sys)
  (-> (store/dispatch! store act/profile:load!)
      (v/and (ws/init! sys) (render [main/root sys]))
      (v/or (render [login/root sys]))
      (v/always (log/info [:app/initialized]))))

(defn find-component [system k]
  (some-> system (ig/find-derived-1 k) val))

(defn init
  "runs when the browser page has loaded"
  ([]
   (set! log/*ctx* {:disabled? true})
   (init (ig/init config [:system.ui/components])))
  ([system]
   (if-let [sys (find-component system :system.ui/components)]
     (init* sys)
     (throw (ex-info "bad system" {:system system})))))
