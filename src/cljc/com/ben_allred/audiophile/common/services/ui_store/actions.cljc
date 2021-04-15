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

(defn toast! [level msg]
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
                                                  msg)}]))))

(defn server-err! [err-code]
  (toast! :error (keyword err-code)))
