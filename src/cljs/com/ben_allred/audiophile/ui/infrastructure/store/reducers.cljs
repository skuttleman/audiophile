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

(def reducer
  (rcollaj/combine {:nav/route    route
                    :user/profile user}))
