(ns audiophile.ui.store.mutations
  (:require
    [audiophile.common.core.utils.logger :as log]
    [audiophile.common.core.utils.maps :as maps]
    [audiophile.common.infrastructure.store.core :as store]))

(defmethod store/mutate* :banner/create
  [state [_ {:keys [id] :as banner}]]
  (assoc-in state [:banners id] banner))

(defmethod store/mutate* :banner/clear
  [state [_ {:keys [id]}]]
  (update state :banners dissoc id))

(defmethod store/mutate* :modal/create
  [state [_ {:keys [id] :as frame}]]
  (assoc-in state [:modals id] (assoc frame :state :init)))

(defmethod store/mutate* :modal/display
  [state [_ {:keys [id]}]]
  (maps/update-in-maybe state [:modals id] assoc :state :showing))

(defmethod store/mutate* :modal/hide
  [state [_ {:keys [id]}]]
  (maps/update-in-maybe state [:modals id] assoc :state :removing))

(defmethod store/mutate* :modal/hide-all
  [state _]
  (update state :modals (partial into
                                 {}
                                 (map (fn [[k v]]
                                        [k (assoc v :state :removing)])))))

(defmethod store/mutate* :modal/clear
  [state [_ {:keys [id]}]]
  (update state :modals dissoc id))

(defmethod store/mutate* :modal/clear-all
  [state _]
  (update state :modals empty))

(defmethod store/mutate* :profile/set
  [state [_ profile]]
  (assoc state :user/profile profile))

(defmethod store/mutate* :form/cleanup
  [state [_ {:keys [id]}]]
  (update state :forms dissoc id))

(defmethod store/mutate* :form/init
  [state [_ {:keys [id data]}]]
  (assoc-in state [:forms id] data))

(defmethod store/mutate* :form/update
  [state [_ {:keys [id f]}]]
  (maps/update-in-maybe state [:forms id] f))

(defmethod store/mutate* :resource/init
  [state [_ {:keys [id data]}]]
  (assoc-in state [:resources id] data))

(defmethod store/mutate* :resource/remove
  [state [_ {:keys [id]}]]
  (update state :resources dissoc id))

(defmethod store/mutate* :resource/set
  [state [_ {:keys [id data]}]]
  (maps/update-in-maybe state [:resources id] (constantly data)))

(defmethod store/mutate* :toast/create
  [state [_ {:keys [id] :as toast}]]
  (assoc-in state [:toasts id] (assoc toast :state :init)))

(defmethod store/mutate* :toast/display
  [state [_ {:keys [id]}]]
  (maps/update-in-maybe state [:toasts id] assoc :state :showing))

(defmethod store/mutate* :toast/hide
  [state [_ {:keys [id]}]]
  (maps/update-in-maybe state [:toasts id] assoc :state :removing))

(defmethod store/mutate* :toast/clear
  [state [_ {:keys [id]}]]
  (update state :toasts dissoc id))
