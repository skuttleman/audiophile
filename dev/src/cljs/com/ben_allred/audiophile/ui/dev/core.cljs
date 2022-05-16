(ns com.ben-allred.audiophile.ui.dev.core
  (:require
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.ui.app :as app]
    [com.ben-allred.audiophile.ui.infrastructure.system.core :as sys]
    [integrant.core :as ig]))

(defonce ^:private sys
  (atom nil))

(def ^:private config
  (sys/load-config "ui-dev.edn" [:duct.profile/base :duct.profile/dev]))

(defn ^:export init []
  (reset! sys (try (ig/init config)
                   (catch :default ex
                     (log/error ex "ERROR!!!")
                     nil)))
  (app/init @sys))

(defn ^:export halt []
  (some-> sys deref ig/halt!))
