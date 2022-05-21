(ns com.ben-allred.audiophile.ui.infrastructure.store.actions
  (:require
    [com.ben-allred.audiophile.common.core.utils.maps :as maps]))

(def ^:private banner-err-codes
  {:login-failed "Authentication failed. Please try again."})

(defn banner:add! [level code]
  (if-let [body (banner-err-codes code)]
    [:banners/add! (maps/->m {:id (.getTime (js/Date.))} level body)]
    (throw (ex-info "banners must have a body" {:level level :cod code}))))

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

(defn profile:set! [profile]
  [:user.profile/set! profile])

(defn router:update [route]
  [:router/update route])
