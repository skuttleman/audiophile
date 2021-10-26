(ns com.ben-allred.audiophile.ui.core.utils.dom
  (:require
    [clojure.set :as set]))

(def ^:private listeners (atom {}))

(def ^:private key->code
  {:key-codes/tab   9
   :key-codes/esc   27
   :key-codes/enter 13})

(def ^:private code->key
  (set/map-invert key->code))

(def ^:const window js/window)

(def ^:const document js/document)

(defn stop-propagation [event]
  (when (some-> event .-stopPropagation)
    (.stopPropagation event))
  event)

(defn prevent-default [event]
  (when (some-> event .-preventDefault)
    (.preventDefault event))
  event)

(defn target-value [event]
  (some-> event .-target .-value not-empty))

(defn query-one [selector]
  (.querySelector document selector))

(defn click [node]
  (.click node))

(defn blur [node]
  (.blur node))

(defn focus [node]
  (.focus node)
  (when (some-> node .-setSelectionRange)
    (let [length (-> node .-value .-length)]
      (.setSelectionRange node length length))))

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

(defn assign! [path]
  (.assign (.-location window)
           path)
  nil)

