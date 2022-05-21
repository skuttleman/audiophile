(ns audiophile.ui.utils.dom
  (:require
    [clojure.set :as set]))

(defonce ^:private listeners (atom {}))

(def ^:private key->code
  {:key-codes/tab   9
   :key-codes/esc   27
   :key-codes/enter 13})

(def ^:private code->key
  (set/map-invert key->code))

(defn assign! [url]
  (.assign (.-location js/window) url)
  nil)

(defn prevent-default! [e]
  (some-> e .preventDefault)
  e)

(defn stop-propagation! [e]
  (some-> e .stopPropagation)
  e)

(defn target-value [e]
  (some-> e .-target .-value))

(defn click! [node]
  (some-> node .click)
  node)

(defn blur! [node]
  (some-> node .blur)
  node)

(defn focus! [node]
  (some-> node .focus)
  node)

(defn event->key [e]
  (some-> e .-keyCode code->key))

(defn add-listener
  ([node event cb]
   (add-listener node event cb nil))
  ([node event cb options]
   (let [key (gensym)
         listener [node event (.addEventListener node (name event) cb (clj->js options))]]
     (swap! listeners assoc key listener)
     key)))

(defn remove-listener [key]
  (when-let [[node event id] (get @listeners key)]
    (swap! listeners dissoc key)
    (.removeEventListener node (name event) id)))
