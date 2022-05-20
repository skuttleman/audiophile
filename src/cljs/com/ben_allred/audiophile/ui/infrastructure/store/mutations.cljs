(ns com.ben-allred.audiophile.ui.infrastructure.store.mutations
  (:require [com.ben-allred.audiophile.common.infrastructure.store.core :as store]))

(defmethod store/reduce* :banners/add!
  [state [_ {:keys [id] :as banner}]]
  (assoc-in state [:banners id] banner))

(defmethod store/reduce* :banners/remove!
  [state [_ {:keys [id]}]]
  (update state :banners dissoc id))

(defmethod store/reduce* :router/update
  [state [_ route]]
  (assoc state :nav/route route))

(defmethod store/reduce* :user.profile/set!
  [state [_ profile]]
  (assoc state :user/profile profile))
