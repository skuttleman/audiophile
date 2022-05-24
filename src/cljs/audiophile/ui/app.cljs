(ns audiophile.ui.app
  (:require
    [audiophile.common.core.utils.logger :as log]
    [audiophile.common.infrastructure.store.core :as store]
    [audiophile.ui.services.ws :as ws]
    [audiophile.ui.store.actions :as act]
    [audiophile.ui.system.core :as sys]
    [audiophile.ui.utils.modulizer :as mod]
    [com.ben-allred.vow.core :as v :include-macros true]
    [integrant.core :as ig]
    [reagent.dom :as rdom]
    audiophile.ui.store.async
    audiophile.ui.store.mutations))

(def layout (mod/lazy-component audiophile.ui.views.layout/root))

(def login (mod/lazy-component audiophile.ui.views.login.core/root))

(def ^:private config
  (sys/load-config "ui.edn" [:duct.profile/base :duct.profile/prod]))

(defn ^:private render [comp]
  (v/create (fn [resolve _]
              (rdom/render comp
                           (.getElementById js/document "root")
                           resolve))))

(defn ^:private init* [{:keys [login-key store] :as sys}]
  (store/init! store sys)
  (-> (store/dispatch! store act/profile#load!)
      (v/peek (fn [[status]]
                (let [profile (:user/profile @store)
                      view (cond
                             (= :error status)
                             [@login sys {:msg "Login to get started" :login-key login-key}]

                             (contains? (:jwt/aud profile) :token/signup)
                             (do
                               (ws/init! sys)
                               [@login sys {:msg "Sign up to get started" :login-key :signup}])

                             :else
                             (do
                               (ws/init! sys)
                               [@layout sys]))]
                  (v/always (render view) (log/info [:app/initialized])))))))

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
