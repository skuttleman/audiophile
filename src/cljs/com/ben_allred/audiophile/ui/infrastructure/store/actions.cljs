(ns com.ben-allred.audiophile.ui.infrastructure.store.actions
  (:require
    [clojure.core.async :as async]
    [com.ben-allred.audiophile.ui.infrastructure.store.core :as store]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.common.core.utils.maps :as maps]))

(defn remove-toast!
  ([id]
   (remove-toast! id 1000))
  ([id lag-ms]
   (fn [store]
     (async/go
       (store/dispatch! store [:toasts/hide! {:id id}])
       (async/<! (async/timeout lag-ms))
       (store/dispatch! store [:toasts/remove! {:id id}])))))

(defn toast!
  ([level body]
   (toast! level body 6000 1000))
  ([level body linger-ms lag-ms]
   (fn [store]
     (let [id (.getTime (js/Date.))
           body (delay
                  (async/go
                    (store/dispatch! store [:toasts/display! {:id id}])
                    (async/<! (async/timeout linger-ms))
                    (store/dispatch! store (remove-toast! id lag-ms)))
                  body)]
       (store/dispatch! store [:toasts/add! {:id    id
                                                :level level
                                                :body  body}])
       id))))

(defn remove-banner! [id]
  [:banners/remove! {:id id}])

(defn banner! [level body]
  (fn [store]
    (let [id (.getTime (js/Date.))]
      (store/dispatch! store
                          [:banners/add! {:id    id
                                          :level level
                                          :body  body}])
      id)))

(defn remove-modal! [id]
  (fn [store]
    (store/dispatch! store [:modals/hide! id])
    (async/go
      (async/<! (async/timeout 500))
      (store/dispatch! store [:modals/remove! id]))))

(defn remove-modal-all! [store]
  (store/dispatch! store [:modals/hide-all!])
  (async/go
    (async/<! (async/timeout 500))
    (store/dispatch! store [:modals/remove-all!])))

(defn modal!
  ([body]
   (modal! nil body))
  ([header body]
   (modal! header body nil))
  ([header body buttons]
   (fn [store]
     (let [id (.getTime (js/Date.))
           body (cond-> body (not (vector? body)) vector)]
       (store/dispatch! store [:modals/add! id (maps/->m header body buttons)])
       (async/go
         (async/<! (async/timeout 10))
         (store/dispatch! store [:modals/display! id]))
       id))))
