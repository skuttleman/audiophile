(ns com.ben-allred.audiophile.common.services.ui-store.actions
  (:require
    [clojure.core.async :as async]
    [com.ben-allred.audiophile.common.services.ui-store.core :as ui-store]
    [com.ben-allred.audiophile.common.utils.maps :as maps])
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

(defn remove-modal! [id]
  (fn [store]
    (ui-store/dispatch! store [:modals/hide! id])
    (async/go
      (async/<! (async/timeout 2000))
      (ui-store/dispatch! store [:modals/remove! id]))))

(defn remove-modal-all! [store]
  (ui-store/dispatch! store [:modals/hide-all!])
  (async/go
    (async/<! (async/timeout 2000))
    (ui-store/dispatch! store [:modals/remove-all!])))

(defn modal!
  ([body]
   (modal! nil body))
  ([header body]
   (modal! header body nil))
  ([header body buttons]
   (fn [store]
     (let [id (.getTime #?(:cljs (js/Date.) :default (Date.)))]
       (ui-store/dispatch! store [:modals/add! id (maps/->m header body buttons)])
       (async/go
         (async/<! (async/timeout 10))
         (ui-store/dispatch! store [:modals/display! id]))
       id))))
