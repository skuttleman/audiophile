(ns com.ben-allred.audiophile.common.services.ui-store.actions
  (:require
    [clojure.core.async :as async]
    [com.ben-allred.audiophile.common.services.ui-store.core :as ui-store])
  #?(:clj
     (:import
       (java.util Date))))

(defn remove-toast! [id]
  (fn [store]
    (async/go
      (ui-store/dispatch! store [:toasts/hide! {:id id}])
      (async/<! (async/timeout 1000))
      (ui-store/dispatch! store [:toasts/remove! {:id id}]))))

(defn toast! [level body]
  (fn [store]
    (let [id (.getTime #?(:cljs (js/Date.) :default (Date.)))]
      (ui-store/dispatch! store
                          [:toasts/add! {:id    id
                                         :level level
                                         :body  (delay
                                                  (async/go
                                                    (ui-store/dispatch! store [:toasts/display! {:id id}])
                                                    (async/<! (async/timeout 6000))
                                                    (ui-store/dispatch! store (remove-toast! id)))
                                                  body)}]))))

(defn remove-banner! [id]
  [:banners/remove! {:id id}])

(defn banner! [level body]
  (fn [store]
    (let [id (.getTime #?(:cljs (js/Date.) :default (Date.)))]
      (ui-store/dispatch! store
                          [:banners/add! {:id    id
                                          :level level
                                          :body  body}]))))

(defn server-err! [err-code]
  (banner! :error (keyword err-code)))
