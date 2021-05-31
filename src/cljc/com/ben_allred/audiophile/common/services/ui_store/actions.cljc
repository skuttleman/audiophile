(ns com.ben-allred.audiophile.common.services.ui-store.actions
  (:require
    [clojure.core.async :as async]
    [com.ben-allred.audiophile.common.services.ui-store.core :as ui-store]
    [com.ben-allred.audiophile.common.utils.logger :as log]
    [com.ben-allred.audiophile.common.utils.maps :as maps])
  #?(:clj
     (:import
       (java.util Date))))

(defn remove-toast!
  ([id]
   (remove-toast! id 1000))
  ([id lag-ms]
   (fn [store]
     (async/go
       (ui-store/dispatch! store [:toasts/hide! {:id id}])
       (async/<! (async/timeout lag-ms))
       (ui-store/dispatch! store [:toasts/remove! {:id id}])))))

(defn toast!
  ([level body]
   (toast! level body 6000 1000))
  ([level body linger-ms lag-ms]
   (fn [store]
     (let [id (.getTime #?(:cljs (js/Date.) :default (Date.)))
           body (delay
                  (async/go
                    (ui-store/dispatch! store [:toasts/display! {:id id}])
                    (async/<! (async/timeout linger-ms))
                    (ui-store/dispatch! store (remove-toast! id lag-ms)))
                  body)]
       (ui-store/dispatch! store [:toasts/add! {:id    id
                                                :level level
                                                :body  body}])))))

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
      (async/<! (async/timeout 500))
      (ui-store/dispatch! store [:modals/remove! id]))))

(defn remove-modal-all! [store]
  (ui-store/dispatch! store [:modals/hide-all!])
  (async/go
    (async/<! (async/timeout 500))
    (ui-store/dispatch! store [:modals/remove-all!])))

(defn modal!
  ([body]
   (modal! nil body))
  ([header body]
   (modal! header body nil))
  ([header body buttons]
   (fn [store]
     (let [id (.getTime #?(:cljs (js/Date.) :default (Date.)))
           body (cond-> body (not (vector? body)) vector)]
       (ui-store/dispatch! store [:modals/add! id (maps/->m header body buttons)])
       (async/go
         (async/<! (async/timeout 10))
         (ui-store/dispatch! store [:modals/display! id]))
       id))))
