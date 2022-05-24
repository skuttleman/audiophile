(ns audiophile.common.core.stubs.pushy
  #?(:cljs
     (:require
       [pushy.core :as pushy*])))

(defn start! [pushy]
  #?(:cljs (pushy*/start! pushy)))

(defn stop! [pushy]
  #?(:cljs (pushy*/stop! pushy)))

(defn set-token! [pushy token]
  #?(:cljs (pushy*/set-token! pushy token)))

(defn replace-token! [pushy token]
  #?(:cljs (pushy*/replace-token! pushy token)))

(defn pushy [dispatch-fn match-fn]
  #?(:cljs (pushy*/pushy dispatch-fn match-fn)))
