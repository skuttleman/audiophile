(ns audiophile.ui.store.actions
  (:require
    [audiophile.common.core.utils.maps :as maps]))

(def ^:private banner-err-codes
  {:login-failed "Authentication failed. Please try again."})

(defn banner:create [level code]
  (let [body (banner-err-codes code "An error occurred. Please try again.")]
    [:banner/create (maps/->m {:id (.getTime (js/Date.))} level body)]))

(defn banner:clear [id]
  [:banner/clear {:id id}])

(defn form:cleanup [id]
  [:form/cleanup {:id id}])

(defn form:merge [id data]
  [:form/merge (maps/->m id data)])

(defn form:update [id f & f-args]
  [:form/update {:id id :f #(apply f % f-args)}])

(defn modal#add!
  ([body]
   (modal#add! nil body))
  ([header body]
   (let [id (.getTime (js/Date.))
         body (cond-> body (not (vector? body)) vector)]
     [::modal#add! (maps/->m id header body)])))

(defn modal#remove! [id]
  [::modal#remove! {:id id}])

(def modal#remove-all!
  [::modal#remove-all!])

(defn modal:create [frame]
  [:modal/create frame])

(defn modal:display [frame]
  [:modal/display frame])

(defn modal:hide [frame]
  [:modal/hide frame])

(def modal:hide-all
  [:modal/hide-all])

(defn modal:clear [frame]
  [:modal/clear frame])

(def modal:clear-all
  [:modal/clear-all])

(def profile#load!
  [::profile#load!])

(defn profile:set [profile]
  [:profile/set profile])

(defn router:update [route]
  [:router/update route])

(defn toast#add! [level body]
  (let [id (.getTime (js/Date.))]
    [::toast#add! (maps/->m id body level)]))

(defn toast#remove! [id]
  [::toast#remove! {:id id}])

(defn toast:add [toast]
  [:toast/create toast])

(defn toast:display [id]
  [:toast/display {:id id}])

(defn toast:hide [id]
  [:toast/hide {:id id}])

(defn toast:clear [id]
  [:toast/clear {:id id}])
