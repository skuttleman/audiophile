(ns audiophile.ui.forms.watchable
  (:require
    [audiophile.common.core.utils.logger :as log]
    [audiophile.common.core.utils.maps :as maps]
    [audiophile.common.core.utils.uuids :as uuids]
    [audiophile.common.infrastructure.protocols :as pcom]
    [audiophile.common.infrastructure.store.core :as store]
    [audiophile.ui.forms.protocols :as pforms]
    [audiophile.ui.store.actions :as act]
    [audiophile.ui.store.queries :as q]
    [reagent.core :as r]))

(defn ^:private notify! [*form watchers old new]
  (doseq [[k f] watchers]
    (f k *form old new)))

(deftype WatchableForm [id store watchers]
  pcom/IIdentify
  (id [_]
    id)

  pforms/IInit
  (init! [_ value]
    (let [rep (maps/flatten value)]
      (store/dispatch! store (act/form:init id {:current rep}))))

  pcom/IDestroy
  (destroy! [_]
    (store/dispatch! store (act/form:cleanup id)))

  pforms/IChange
  (change! [this path value]
    (let [old @this
          [f args] (if (some? value)
                     [assoc-in [[:current path] value]]
                     [update [:current dissoc path]])]
      (store/dispatch! store (apply act/form:update id f args))
      (-notify-watches this old @this)))
  (changed? [_]
    (let [{:keys [current init]} (q/form:state store id)]
      (= current init)))
  (changed? [_ path]
    (let [{:keys [current init]} (q/form:state store id)]
      (= (get current path) (get init path))))

  IDeref
  (-deref [_]
    (-> store
        (q/form:state id)
        :current
        maps/nest))

  IWatchable
  (-notify-watches [this old-val new-val]
    (notify! this @watchers old-val new-val))
  (-add-watch [_ key f]
    (swap! watchers assoc key f))
  (-remove-watch [_ key]
    (swap! watchers dissoc key)))

(defn create
  ([store]
   (create store nil))
  ([store init-value]
   (create (uuids/random) store init-value))
  ([id store init-value]
   {:pre [(or (nil? init-value) (map? init-value))]}
   (doto (->WatchableForm id store (r/atom {}))
     (pforms/init! init-value))))
