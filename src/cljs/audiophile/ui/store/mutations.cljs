(ns audiophile.ui.store.mutations
  (:require
    [audiophile.common.core.utils.logger :as log]
    [audiophile.common.core.utils.maps :as maps]
    [audiophile.common.infrastructure.store.core :as store]))

(defmethod store/mutate* :banners/add!
  [state [_ {:keys [id] :as banner}]]
  (assoc-in state [:banners id] banner))

(defmethod store/mutate* :banners/remove!
  [state [_ {:keys [id]}]]
  (update state :banners dissoc id))

(defmethod store/mutate* :modals/add!
  [state [_ {:keys [id] :as frame}]]
  (assoc-in state [:modals id] (assoc frame :state :init)))

(defmethod store/mutate* :modals/display!
  [state [_ {:keys [id]}]]
  (maps/update-in-maybe state [:modals id] assoc :state :showing))

(defmethod store/mutate* :modals/hide!
  [state [_ {:keys [id]}]]
  (maps/update-in-maybe state [:modals id] assoc :state :removing))

(defmethod store/mutate* :modals/hide-all!
  [state _]
  (update state :modals (partial into
                                 {}
                                 (map (fn [[k v]]
                                        [k (assoc v :state :removing)])))))

(defmethod store/mutate* :modals/remove!
  [state [_ {:keys [id]}]]
  (update state :modals dissoc id))

(defmethod store/mutate* :modals/remove-all!
  [state _]
  (update state :modals empty))

(defmethod store/mutate* :router/update
  [state [_ route]]
  (assoc state :nav/route route))

(defmethod store/mutate* :toasts/add!
  [state [_ {:keys [id] :as toast}]]
  (assoc-in state [:toasts id] (assoc toast :state :init)))

(defmethod store/mutate* :toasts/display!
  [state [_ {:keys [id]}]]
  (maps/update-in-maybe state [:toasts id] assoc :state :showing))

(defmethod store/mutate* :toasts/hide!
  [state [_ {:keys [id]}]]
  (maps/update-in-maybe state [:toasts id] assoc :state :removing))

(defmethod store/mutate* :toasts/remove!
  [state [_ {:keys [id]}]]
  (update state :toasts dissoc id))

(defmethod store/mutate* :user.profile/set!
  [state [_ profile]]
  (assoc state :user/profile profile))
