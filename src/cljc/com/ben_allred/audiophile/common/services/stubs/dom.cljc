(ns com.ben-allred.audiophile.common.services.stubs.dom
  (:require
    [clojure.set :as set]))

(def ^:private listeners (atom {}))

(def ^:private key->code
  {:key-codes/tab   9
   :key-codes/esc   27
   :key-codes/enter 13})

(def ^:private code->key
  (set/map-invert key->code))

(def ^:const window #?(:cljs js/window))

(def ^:const document #?(:cljs js/document))

(defn stop-propagation [event]
  #?(:cljs
     (when (.-stopPropagation event)
       (.stopPropagation event)))
  event)

(defn prevent-default [event]
  #?(:cljs
     (when (.-preventDefault event)
       (.preventDefault event)))
  event)

(defn target-value [event]
  #?(:cljs
     (some-> event
             (.-target)
             (.-value))))

(defn query-one [selector]
  #?(:cljs
     (.querySelector document selector)))

(defn click [node]
  #?(:cljs
     (.click node)))

(defn blur [node]
  #?(:cljs
     (.blur node)))

(defn focus [node]
  #?@(:cljs
      [(.focus node)
       (when (.-setSelectionRange node)
         (let [length (-> node .-value .-length)]
           (.setSelectionRange node length length)))]))

(defn event->key [e]
  #?(:cljs
     (-> e .-keyCode code->key)))

(defn add-listener
  ([node event cb]
   (add-listener node event cb nil))
  ([node event cb options]
   #?(:cljs
      (let [key (gensym)
            listener [node event (.addEventListener node (name event) cb (clj->js options))]]
        (swap! listeners assoc key listener)
        key))))

(defn remove-listener [key]
  #?(:cljs
     (when-let [[node event id] (get @listeners key)]
       (swap! listeners dissoc key)
       (.removeEventListener node (name event) id))))

(defn assign! [path]
  #?(:cljs
     (.assign (.-location window)
              path))
  nil)
