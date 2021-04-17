(ns com.ben-allred.audiophile.common.services.ui-store.reducers
  (:require
    [com.ben-allred.audiophile.common.utils.maps :as maps]
    [com.ben-allred.collaj.reducers :as rcollaj]))

(defn ^:private page
  ([] nil)
  ([state [type route]]
   (case type
     :router/updated route
     state)))

(defn ^:private toasts
  ([] {})
  ([state [type {:keys [id level body]}]]
   (case type
     :toasts/add! (assoc state id {:state :init :level level :body body})
     :toasts/display! (maps/update-maybe state id assoc :state :showing)
     :toasts/hide! (maps/update-maybe state id assoc :state :removing)
     :toasts/remove! (dissoc state id)
     state)))

(defn ^:private banners
  ([] {})
  ([state [type {:keys [id level body]}]]
   (case type
     :banners/add! (assoc state id {:level level :body body})
     :banners/remove! (dissoc state id)
     state)))

(def reducer
  (rcollaj/combine {:banners banners
                    :page    page
                    :toasts  toasts}))
