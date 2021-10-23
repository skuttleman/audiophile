(ns com.ben-allred.audiophile.ui.infrastructure.store.reducers
  (:require
    [com.ben-allred.audiophile.common.core.utils.maps :as maps]
    [com.ben-allred.collaj.reducers :as rcollaj]))

(defn ^:private ident*
  ([] nil)
  ([state _] state))

(defn route
  ([] nil)
  ([state [type route]]
   (case type
     :router/updated route
     state)))

(defn toasts
  ([] {})
  ([state [type {:keys [id level body]}]]
   (case type
     :toasts/add! (assoc state id {:state :init :level level :body body})
     :toasts/display! (maps/update-maybe state id assoc :state :showing)
     :toasts/hide! (maps/update-maybe state id assoc :state :removing)
     :toasts/remove! (dissoc state id)
     state)))

(defn banners
  ([] {})
  ([state [type {:keys [id key level body]}]]
   (case type
     :banners/add! (cond-> state
                     (not (contains? (::keys (meta state)) key))
                     (-> (assoc id (maps/->m body level key))
                         (cond->
                           key (vary-meta update ::keys (fnil conj #{}) key))))
     :banners/remove! (-> state
                          (dissoc id)
                          (vary-meta update ::keys disj (get-in state [id :key])))
     state)))

(defn modals
  ([] {})
  ([state [type id frame]]
   (case type
     :modals/add! (assoc state id (assoc frame :state :init))
     :modals/display! (maps/update-maybe state id assoc :state :showing)
     :modals/hide! (maps/update-maybe state id assoc :state :removing)
     :modals/hide-all! (into state
                             (map (fn [[k v]]
                                    [k (assoc v :state :removing)]))
                             state)
     :modals/remove! (dissoc state id)
     :modals/remove-all! (empty state)
     state)))

(def reducer
  (rcollaj/combine (maps/->m {:auth/user ident*
                              :nav/route route}
                             banners
                             modals
                             toasts)))
