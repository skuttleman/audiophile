(ns com.ben-allred.audiophile.ui.infrastructure.modulizer
  #?(:cljs
     (:require-macros
       com.ben-allred.audiophile.ui.infrastructure.modulizer))
  (:require
    [reagent.core :as r]
    [shadow.lazy :as lazy]))

(defn load* [loadable]
  (fn [& _]
    #?(:cljs (let [*comp (r/atom nil)]
               (-> loadable
                   lazy/load
                   (.then (partial reset! *comp)))
               (fn [& args]
                 (if-let [comp @*comp]
                   (into [comp] args)
                   [:div.loader.medium]))))))

(defmacro lazy-component [the-sym]
  `(delay (load* (lazy/loadable ~the-sym))))
