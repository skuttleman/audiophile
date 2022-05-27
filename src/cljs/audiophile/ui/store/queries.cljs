(ns audiophile.ui.store.queries)

(defn banner:state [store]
  (:banners @store))

(defn form:state [store id]
  (get-in @store [:forms id]))

(defn modal:state [store]
  (:modals @store))

(defn nav:route [store]
  (:nav/route @store))

(defn res:state [store id]
  (get-in @store [:resources id]))

(defn toast:state [store]
  (:toasts @store))

(defn user:profile [store]
  (:user/profile @store))
