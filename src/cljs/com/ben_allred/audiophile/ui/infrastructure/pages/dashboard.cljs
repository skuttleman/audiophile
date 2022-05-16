(ns com.ben-allred.audiophile.ui.infrastructure.pages.dashboard
  (:require
    [com.ben-allred.audiophile.common.core.utils.logger :as log]))

(println "DASHBOARD LOADED")

(defn page [& args]
  [:div "DASHBOARD3"
   [log/pprint args]])
