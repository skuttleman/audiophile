(ns com.ben-allred.audiophile.ui.infrastructure.store.reducers
  (:require
    [com.ben-allred.audiophile.common.core.utils.maps :as maps]
    [com.ben-allred.collaj.reducers :as rcollaj]))

(defn route
  ([] nil)
  ([state [type route]]
   (case type
     :router/navigate! route
     state)))

(defn user
  ([] nil)
  ([state [type profile]]
   (case type
     :user/profile profile
     state)))

(defn banners
  ([] {})
  ([state [type {:keys [id] :as banner}]]
   (case type
     :banners/add! (assoc state id banner)
     :banners/remove! (dissoc state id)
     state)))

(def reducer
  (rcollaj/combine (maps/->m {:nav/route    route
                              :user/profile user}
                             banners)))
