(ns com.ben-allred.audiophile.ui.dev.core
  (:require
    [com.ben-allred.audiophile.ui.app :as app]))

(defonce ^:private sys
  (atom nil))

(defn ^:export init []
  #_(reset! sys (try (ig/init config)
                     (catch :default ex
                       (log/error ex "ERROR!!!")
                       nil)))
  (app/init @sys))

(defn ^:export halt []
  #_(some-> sys deref ig/halt!))
