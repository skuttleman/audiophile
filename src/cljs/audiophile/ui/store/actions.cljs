(ns audiophile.ui.store.actions
  (:require
    [audiophile.common.core.utils.maps :as maps]))

(def ^:private banner-err-codes
  {:login-failed "Authentication failed. Please try again."})

(defn banner:add! [level code]
  (let [body (banner-err-codes code "An error occurred. Please try again.")]
    [:banners/add! (maps/->m {:id (.getTime (js/Date.))} level body)]))

(defn banner:remove! [id]
  [:banners/remove! {:id id}])

(defn modal:add!
  ([body]
   (modal:add! nil body))
  ([header body]
   (let [id (.getTime (js/Date.))
         body (cond-> body (not (vector? body)) vector)]
     [::modal:add! (maps/->m id header body)])))

(defn modal:remove! [id]
  [::modal:remove! {:id id}])

(def modal:remove-all!
  [::modal:remove-all!])

(def profile:load!
  [::user:load-profile!])

(defn toast:add! [level body]
  (let [id (.getTime (js/Date.))]
    [::toast:add! (maps/->m id body level)]))

(defn toast:remove! [id]
  [::toast:remove! {:id id}])
